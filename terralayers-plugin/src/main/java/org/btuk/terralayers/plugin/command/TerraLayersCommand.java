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
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;

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

    private void info(CommandSender sender, String label) {
        sender.sendMessage(Component.text("TerraLayers", NamedTextColor.GOLD).append(Component.text(plugin.getPluginMeta().getVersion(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("/" + label + " reload", NamedTextColor.GRAY).append(Component.text(" - reload config and reinitialize services", NamedTextColor.DARK_GRAY)));
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(Component.text("You do not have permission to do that.", NamedTextColor.RED));
    }

    private void initLayers(CommandSender sender) {
        long start = System.currentTimeMillis();

        // Assert no layers exist.
        if (!layerManager.getLayers().isEmpty()) {
            sender.sendMessage(Component.text("Cannot initialize layers: already initialized", NamedTextColor.RED));
            return;
        }

        // Get the min and max y.
        int minY = configManager.getGlobalMin();
        int maxY = configManager.getGlobalMax();

        int worldHeight = configManager.getWorldHeight();
        int bufferSize = configManager.getBufferSize();

        // Validate the min and max y.
        boolean valid = (maxY - minY) > 0;
        valid &= worldHeight % 16 == 0;
        valid &= bufferSize % 16 == 0;
        valid &= (maxY - minY) % worldHeight == 0;
        valid &= maxY % worldHeight == 0;
        valid &= minY % worldHeight == 0;

        if (!valid) {
            sender.sendMessage(Component.text("Invalid layer configuration, please check the configured values for worldHeight, bufferSize, globalMin and globalMax", NamedTextColor.RED));
            return;
        }

        // Get the name of the current default world.
        Properties serverProperties = loadServerProperties();
        String defaultWorldName = serverProperties.getProperty("level-name");

        if (defaultWorldName == null) {
            sender.sendMessage(Component.text("Failed to load server.properties, please check the server logs for more information", NamedTextColor.RED));
            return;
        }

        DatapackManager datapackManager = new DatapackManager(plugin.getLogger(), -bufferSize, worldHeight + 2 * bufferSize);
        String minecraftVersion = plugin.getServer().getMinecraftVersion();

        // Create the datapack for the world y-range.
        Datapack datapack = datapackManager.createDatapack(minecraftVersion);

        if (!saveAndEnableDatapack(datapackManager, datapack, sender, defaultWorldName)) {
            return;
        }

        // Create a world for each layer.
        String worldBaseName = configManager.getWorldBaseName();
        List<CompletableFuture<LoadedMultiverseWorld>> worldFutures = new ArrayList<>();
        int delayTicks = 0;
        int delayIncrement = 20;

        for (int y = minY; y < maxY; y += worldHeight) {
            // Create the world using the Multiverse API.
            worldFutures.add(worldManager.createWorld(plugin, sender, worldBaseName + "_" + y + "_" + (y + worldHeight), delayTicks));
            delayTicks += delayIncrement;
        }

        // Wait until the worlds are created.
        CompletableFuture.allOf(worldFutures.toArray(new CompletableFuture[0])).thenRunAsync(() -> {
            List<LoadedMultiverseWorld> worlds = worldFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            // Determine the new default world and update the config to reflect that.
            World defaultWorld = worlds.getFirst().getBukkitWorld().get();

            serverProperties.setProperty("level-name", defaultWorld.getName());
            saveServerProperties(serverProperties);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getServer().setRespawnWorld(defaultWorld);
                worldManager.unloadWorld(defaultWorldName);

                if (!saveAndEnableDatapack(datapackManager, datapack, sender, defaultWorld.getName())) {
                    return;
                }

                long took = System.currentTimeMillis() - start;
                sender.sendMessage(Component.text("TerraLayers initialized (" + took + "ms).", NamedTextColor.GREEN));
                sender.sendMessage(Component.text("Created " + worlds.size() + " worlds, between y " + minY + " and " + maxY, NamedTextColor.GREEN));
                sender.sendMessage(Component.text("Restart the server to apply the changes.", NamedTextColor.GREEN));
            });
        });
    }

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
        // Add the datapack to the server.
        boolean datapackSaved = datapackManager.saveDatapackToWorld(datapack, plugin.getServer().getWorldContainer().toPath(), world);

        // Ensure the datapack is added to the server.
        if (!datapackSaved) {
            sender.sendMessage(Component.text("Failed to create datapack, please check the server logs for more information", NamedTextColor.RED));
            return false;
        }

        // Enable the datapack.
        boolean datapackEnabled = datapackManager.enableDatapack(plugin.getLogger(), datapack);

        if (!datapackEnabled) {
            sender.sendMessage(Component.text("Failed to enable datapack, please check the server logs for more information", NamedTextColor.RED));
            return false;
        }
        return true;
    }
}
