package org.btuk.terralayers.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.btuk.terralayers.datapack.Datapack;
import org.btuk.terralayers.datapack.DatapackManager;
import org.btuk.terralayers.plugin.TerraLayersPlugin;
import org.btuk.terralayers.plugin.config.ConfigManager;
import org.btuk.terralayers.plugin.impl.SimpleLayerManager;
import org.btuk.terralayers.plugin.multiverse.WorldManager;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class TerraLayersCommand {

    private final TerraLayersPlugin plugin;
    private final SimpleLayerManager layerManager;
    private final ConfigManager configManager;
    private final WorldManager worldManager;

    public TerraLayersCommand(TerraLayersPlugin plugin, SimpleLayerManager layerManager, ConfigManager configManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.layerManager = layerManager;
        this.configManager = configManager;
        this.worldManager = worldManager;
    }

    // Command handlers
    public int reload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!sender.hasPermission("terralayers.reload")) {
            noPermission(sender);
            return Command.SINGLE_SUCCESS;
        }
        long start = System.currentTimeMillis();
        plugin.reloadFromDisk();
        long took = System.currentTimeMillis() - start;
        sender.sendMessage(Component.text("TerraLayers config reloaded (" + took + "ms). Version: "
                + plugin.getConfigManager().getCurrentVersion() + " (bundled " + plugin.getConfigManager().getBundledVersion() + ")", NamedTextColor.GREEN));
        if (sender instanceof Player) {
            plugin.getLogger().info(sender.getName() + " reloaded TerraLayers configuration.");
        }
        return Command.SINGLE_SUCCESS;
    }

    public int init(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!sender.hasPermission("terralayers.init")) {
            noPermission(sender);
            return Command.SINGLE_SUCCESS;
        }
        initLayers(sender);
        return Command.SINGLE_SUCCESS;
    }

    // Utility methods
    private void info(CommandSender sender, String label) {
        sender.sendMessage(Component.text("TerraLayers", NamedTextColor.GOLD).append(Component.text(plugin.getPluginMeta().getVersion(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("/" + label + " reload", NamedTextColor.GRAY).append(Component.text(" - reload config and reinitialize services", NamedTextColor.DARK_GRAY)));
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
    }

    // Main init logic
    private void initLayers(CommandSender sender) {
        long start = System.currentTimeMillis();

        if (!isValidInitializationState(sender)) {
            return;
        }

        Properties serverProperties = loadServerProperties();
        String defaultWorldName = serverProperties.getProperty("level-name");
        if (defaultWorldName == null) {
            sender.sendMessage(Component.text("Failed to load server.properties, please check the server logs for more information", NamedTextColor.RED));
            return;
        }

        DatapackManager datapackManager = createDatapackManager();
        Datapack datapack = datapackManager.createDatapack(plugin.getServer().getMinecraftVersion());

        if (!prepareDatapackForWorld(datapackManager, datapack, sender, defaultWorldName)) {
            return;
        }

        List<CompletableFuture<LoadedMultiverseWorld>> worldFutures = createLayeredWorlds(sender);

        CompletableFuture.allOf(worldFutures.toArray(new CompletableFuture[0])).thenRunAsync(() ->
            finalizeInitialization(worldFutures, serverProperties, datapackManager, datapack, sender, defaultWorldName, start));
    }

    // Validation and checks
    private boolean isValidInitializationState(CommandSender sender) {
        if (!layerManager.getLayers().isEmpty()) {
            sender.sendMessage(Component.text("Cannot initialize layers: already initialized", NamedTextColor.RED));
            return false;
        }

        int minY = configManager.getGlobalMin();
        int maxY = configManager.getGlobalMax();
        int worldHeight = configManager.getWorldHeight();
        int bufferSize = configManager.getBufferSize();

        boolean valid = (maxY - minY) > 0 &&
                        worldHeight % 16 == 0 &&
                        bufferSize % 16 == 0 &&
                        (maxY - minY) % worldHeight == 0 &&
                        maxY % worldHeight == 0 &&
                        minY % worldHeight == 0;

        if (!valid) {
            sender.sendMessage(Component.text("Invalid layer configuration, please check the configured values for worldHeight, bufferSize, globalMin and globalMax", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private DatapackManager createDatapackManager() {
        int bufferSize = configManager.getBufferSize();
        int worldHeight = configManager.getWorldHeight();
        return new DatapackManager(plugin.getLogger(), -bufferSize, worldHeight + 2 * bufferSize);
    }

    private boolean prepareDatapackForWorld(DatapackManager datapackManager, Datapack datapack, CommandSender sender, String worldName) {
        return saveAndEnableDatapack(datapackManager, datapack, sender, worldName);
    }

    // World creation
    private List<CompletableFuture<LoadedMultiverseWorld>> createLayeredWorlds(CommandSender sender) {
        List<CompletableFuture<LoadedMultiverseWorld>> worldFutures = new ArrayList<>();
        int minY = configManager.getGlobalMin();
        int maxY = configManager.getGlobalMax();
        int worldHeight = configManager.getWorldHeight();
        String worldBaseName = configManager.getWorldBaseName();

        int delayTicks = 0;
        int delayIncrement = 20;

        for (int y = minY; y < maxY; y += worldHeight) {
            String worldName = worldBaseName + "_" + y + "_" + (y + worldHeight);
            String generator = "Terraplusminus:" + (-y);
            worldFutures.add(worldManager.createWorld(plugin, sender, worldName, generator, delayTicks));
            delayTicks += delayIncrement;
        }
        return worldFutures;
    }

    // Finalization after worlds are created
    private void finalizeInitialization(List<CompletableFuture<LoadedMultiverseWorld>> worldFutures, Properties serverProperties,
                                       DatapackManager datapackManager, Datapack datapack, CommandSender sender,
                                       String oldDefaultWorldName, long startTime) {
        List<LoadedMultiverseWorld> worlds = worldFutures.stream().map(CompletableFuture::join).toList();
        World newDefaultWorld = worlds.getFirst().getBukkitWorld().get();

        updateServerConfig(serverProperties, newDefaultWorld, worlds.getFirst().getGenerator());
        unloadOldWorld(oldDefaultWorldName);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!prepareDatapackForWorld(datapackManager, datapack, sender, newDefaultWorld.getName())) {
                return;
            }

            plugin.getServer().setRespawnWorld(newDefaultWorld);

            long took = System.currentTimeMillis() - startTime;
            sender.sendMessage(Component.text("TerraLayers initialized (" + took + "ms).", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Created " + worlds.size() + " worlds, between y " + configManager.getGlobalMin() + " and " + configManager.getGlobalMax(), NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Restart the server to apply the changes.", NamedTextColor.GREEN));
        });
    }

    private void updateServerConfig(Properties serverProperties, World newDefaultWorld, String generator) {
        serverProperties.setProperty("level-name", newDefaultWorld.getName());
        saveServerProperties(serverProperties);
        updateBukkitYml(newDefaultWorld.getName(), generator);
    }

    private void unloadOldWorld(String worldName) {
        worldManager.unloadWorld(worldName);
    }

    // File operations
    private Properties loadServerProperties() {
        Properties serverProperties = new Properties();
        try {
            Path serverPropertiesPath = plugin.getServer().getWorldContainer().toPath().resolve("server.properties");
            try (FileInputStream fis = new FileInputStream(serverPropertiesPath.toFile())) {
                serverProperties.load(fis);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error loading server.properties: " + e.getMessage());
        }
        return serverProperties;
    }

    private void saveServerProperties(Properties serverProperties) {
        Path serverPropertiesPath = plugin.getServer().getWorldContainer().toPath().resolve("server.properties");
        try (FileOutputStream outputStream = new FileOutputStream(serverPropertiesPath.toFile())) {
            serverProperties.store(outputStream, null);
        } catch (IOException e) {
            plugin.getLogger().severe("Error updating server.properties: " + e.getMessage());
        }
    }

    private boolean saveAndEnableDatapack(DatapackManager datapackManager, Datapack datapack, CommandSender sender, String world) {
        boolean datapackSaved = datapackManager.saveDatapackToWorld(datapack, plugin.getServer().getWorldContainer().toPath(), world);
        if (!datapackSaved) {
            sender.sendMessage(Component.text("Failed to create datapack, please check the server logs for more information", NamedTextColor.RED));
            return false;
        }

        boolean datapackEnabled = datapackManager.enableDatapack(plugin.getLogger(), datapack);
        if (!datapackEnabled) {
            sender.sendMessage(Component.text("Failed to enable datapack, please check the server logs for more information", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private void updateBukkitYml(String worldName, String generator) {
        File serverRoot = plugin.getServer().getWorldContainer().getParentFile();
        File bukkitYml = new File(serverRoot, "bukkit.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(bukkitYml);

        config.set("worlds." + worldName + ".generator", generator);

        try {
            config.save(bukkitYml);
            plugin.getLogger().info("Updated bukkit.yml with generator for world '" + worldName + "'.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save bukkit.yml: " + e.getMessage());
        }
    }
}