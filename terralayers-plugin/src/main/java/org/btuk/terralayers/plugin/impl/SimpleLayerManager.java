package org.btuk.terralayers.plugin.impl;

import org.btuk.terralayers.api.LayerManager;
import org.btuk.terralayers.api.LayeredWorld;
import org.btuk.terralayers.datapack.Datapack;
import org.btuk.terralayers.plugin.TerraLayersPlugin;
import org.btuk.terralayers.plugin.config.ConfigManager;
import org.bukkit.World;

import java.util.*;

/**
 * Minimal placeholder implementation. It does not auto-discover or manage worlds yet.
 * In future work, this will be populated from configuration and world creation logic.
 */
public class SimpleLayerManager implements LayerManager {

    private final TerraLayersPlugin plugin;
    private final int worldHeight;
    private final int bufferSize;
    private final Map<String, LayeredWorld> layers = new HashMap<>();

    public SimpleLayerManager(TerraLayersPlugin plugin, int worldHeight, int bufferSize) {
        this.plugin = plugin;
        this.worldHeight = worldHeight;
        this.bufferSize = bufferSize;
    }

    @Override
    public Collection<LayeredWorld> getLayers() {
        return layers.values();
    }

    @Override
    public Optional<LayeredWorld> getLayerForGlobalY(int globalY) {
        return layers.values().stream()
                .filter(l -> globalY >= l.getMinY() && globalY < l.getMaxY())
                .findFirst();
    }

    @Override
    public LayeredWorld getLayerForWorld(World world) {
        return layers.get(world.getName());
    }

    @Override
    public int getWorldHeight() {
        return worldHeight;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    public void loadLayers(ConfigManager configManager, Datapack datapack) {

        int yMin = configManager.getGlobalMin();
        int yMax = configManager.getGlobalMax();

        int worldHeight = configManager.getWorldHeight();
        int bufferSize = configManager.getBufferSize();

        if (datapack.getWorldHeight() != worldHeight + 2 * bufferSize) {
            plugin.getLogger().warning("Datapack height does not match expected world height (worldHeight + 2 * bufferSize), unable to load layers.");
            return;
        }

        if (datapack.getYMin() != -bufferSize) {
            plugin.getLogger().warning("Datapack yMin does not match expected y-level (-bufferSize), unable to load layers.");
            return;
        }

        String worldBaseName = configManager.getWorldBaseName();
        for (int i = yMin; i < yMax; i += worldHeight) {

            String worldName = worldBaseName + "_" + i + "_" + (i + worldHeight);
            World world = plugin.getServer().getWorld(worldName);

            if (world == null) {
                plugin.getLogger().warning("Failed to load world " + worldName + ", unable to load layers.");
                layers.clear();
                return;
            }

            LayeredWorld layeredWorld = new SimpleLayeredWorld(world, i, i + worldHeight, bufferSize);
            addLayer(layeredWorld);
        }
        plugin.getLogger().info("Loaded " + layers.size() + " layers.");
    }

    private void addLayer(LayeredWorld layer) {
        if (layer == null) {
            throw new IllegalArgumentException("layer cannot be null");
        }
        if (layer.getBufferSize() != this.bufferSize) {
            throw new IllegalArgumentException("layer buffer size must match configured bufferSize");
        }
        if (layer.getMaxY() - layer.getMinY() != this.worldHeight) {
            throw new IllegalArgumentException("layer height must match configured worldHeight");
        }
        this.layers.put(layer.getName(), layer);
    }
}
