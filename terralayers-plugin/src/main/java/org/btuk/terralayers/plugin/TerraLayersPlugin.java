package org.btuk.terralayers.plugin;

import io.papermc.paper.datapack.DatapackManager;
import org.btuk.terralayers.api.LayerManager;
import org.btuk.terralayers.plugin.command.TerraLayersCommand;
import org.btuk.terralayers.plugin.config.ConfigManager;
import org.btuk.terralayers.plugin.impl.SimpleLayerManager;
import org.btuk.terralayers.plugin.listeners.SwitchLayerListener;
import org.btuk.terralayers.plugin.multiverse.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerraLayersPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private SimpleLayerManager layerManager;
    private SwitchLayerListener switchLayerListener;
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

        this.switchLayerListener = new SwitchLayerListener(this, layerManager);

        this.worldManager = new WorldManager();

        // Register command(s)
        PluginCommand terraLayersCommand = getCommand("terralayers");
        if (terraLayersCommand != null) {
            terraLayersCommand.setExecutor(new TerraLayersCommand(this, layerManager, configManager, worldManager));
        }

        getLogger().info("TerraLayers services registered: LayerManager");
    }

    @Override
    public void onDisable() {
        this.switchLayerListener.unregister();

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
        this.layerManager = new SimpleLayerManager(worldHeight, bufferSize);
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
