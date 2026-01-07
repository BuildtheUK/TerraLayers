package org.btuk.terralayers.plugin.listeners;

import org.btuk.terralayers.datapack.Datapack;
import org.btuk.terralayers.datapack.DatapackManager;
import org.btuk.terralayers.plugin.TerraLayersPlugin;
import org.btuk.terralayers.plugin.config.ConfigManager;
import org.btuk.terralayers.plugin.impl.SimpleLayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.HashSet;
import java.util.Set;

public class WorldLoadListener implements TerraLayersListener {

    private final TerraLayersPlugin plugin;
    private final SimpleLayerManager layerManager;
    private final ConfigManager configManager;

    private final Set<String> worlds = new HashSet<>();

    public WorldLoadListener(TerraLayersPlugin plugin, SimpleLayerManager layerManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.layerManager = layerManager;
        this.configManager = configManager;

        // Add all expected worlds to the set.
        int minY = configManager.getGlobalMin();
        int maxY = configManager.getGlobalMax();
        int worldHeight = configManager.getWorldHeight();
        String worldBaseName = configManager.getWorldBaseName();
        for (int y = minY; y < maxY; y += worldHeight) {
            worlds.add(worldBaseName + "_" + y + "_" + (y + worldHeight));
        }

        this.register();
    }

    private void register() {
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void unregister() {
        WorldLoadEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        // Remove the world from the set.
        worlds.remove(event.getWorld().getName());
        if (worlds.isEmpty()) {
            try {
                DatapackManager datapackManager = new DatapackManager(plugin.getLogger(), -configManager.getBufferSize(), configManager.getWorldHeight() + 2 * configManager.getBufferSize());
                Datapack datapack = datapackManager.loadDatapack(plugin.getServer().getRespawnWorld().getWorldPath());
                layerManager.loadLayers(configManager, datapack);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load layers on world load: " + e.getMessage());
            }
        }
    }
}

