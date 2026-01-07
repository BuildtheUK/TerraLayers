package org.btuk.terralayers.plugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.btuk.terralayers.api.LayerManager;
import org.btuk.terralayers.datapack.Datapack;
import org.btuk.terralayers.datapack.DatapackManager;
import org.btuk.terralayers.plugin.command.TerraLayersCommand;
import org.btuk.terralayers.plugin.config.ConfigManager;
import org.btuk.terralayers.plugin.impl.SimpleLayerManager;
import org.btuk.terralayers.plugin.listeners.SwitchLayerListener;
import org.btuk.terralayers.plugin.listeners.TerraLayersListener;
import org.btuk.terralayers.plugin.listeners.WorldLoadListener;
import org.btuk.terralayers.plugin.multiverse.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class TerraLayersPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private SimpleLayerManager layerManager;
    private final List<TerraLayersListener> listeners = new ArrayList<>();
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        // Load and possibly auto-update configuration
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // Initialize services based on config
        initializeServicesFromConfig();

        // Register services so other plugins (depending on terralayers-api) can use them
        registerServices();

        this.listeners.add(new SwitchLayerListener(this, layerManager, configManager));
        this.listeners.add(new WorldLoadListener(this, layerManager, configManager));

        this.worldManager = new WorldManager();

        // Register command(s)
        TerraLayersCommand terraLayersCommand = new TerraLayersCommand(this, layerManager, configManager, worldManager);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("terralayers")
                    .then(Commands.literal("reload").executes(terraLayersCommand::reload))
                    .then(Commands.literal("init").executes(terraLayersCommand::init));

            LiteralCommandNode<CommandSourceStack> buildCommand = command.build();
            commands.registrar().register(buildCommand);
        });

        getLogger().info("TerraLayers services registered: LayerManager");
    }

    @Override
    public void onDisable() {
        this.listeners.forEach(TerraLayersListener::unregister);

        // Unregister services
        unregisterServices();
    }

    private void registerServices() {
        Bukkit.getServicesManager().register(LayerManager.class, this.layerManager, this, ServicePriority.Normal);
    }

    private void unregisterServices() {
        Bukkit.getServicesManager().unregister(LayerManager.class, this.layerManager);
    }

    public void initializeServicesFromConfig() {
        int worldHeight = configManager.getWorldHeight();
        int bufferSize = configManager.getBufferSize();
        this.layerManager = new SimpleLayerManager(this, worldHeight, bufferSize);
    }

    public void reloadFromDisk() {
        // Reload config and reinitialize services
        this.configManager.reload();
        unregisterServices();
        initializeServicesFromConfig();
        registerServices();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
