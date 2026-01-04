package org.btuk.terralayers.plugin.impl;

import org.btuk.terralayers.api.LayerManager;
import org.btuk.terralayers.api.LayeredWorld;
import org.bukkit.World;

import java.util.*;

/**
 * Minimal placeholder implementation. It does not auto-discover or manage worlds yet.
 * In future work, this will be populated from configuration and world creation logic.
 */
public class SimpleLayerManager implements LayerManager {

    private final int worldHeight;
    private final int bufferSize;
    private final List<LayeredWorld> layers = new ArrayList<>();

    public SimpleLayerManager(int worldHeight, int bufferSize) {
        this.worldHeight = worldHeight;
        this.bufferSize = bufferSize;
    }

    @Override
    public List<LayeredWorld> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    @Override
    public Optional<LayeredWorld> getLayerForGlobalY(int globalY) {
        return layers.stream()
                .filter(l -> globalY >= l.getMinY() && globalY < l.getMaxY())
                .findFirst();
    }

    @Override
    public Optional<LayeredWorld> getLayerForWorld(World world) {
        return layers.stream().filter(l -> l.getWorld().equals(world)).findFirst();
    }

    @Override
    public int getWorldHeight() {
        return worldHeight;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    public void addLayer(LayeredWorld layer) {
        if (layer == null) {
            throw new IllegalArgumentException("layer cannot be null");
        }
        if (layer.getBufferSize() != this.bufferSize) {
            throw new IllegalArgumentException("layer buffer size must match configured bufferSize");
        }
        if (layer.getMaxY() - layer.getMinY() != this.worldHeight) {
            throw new IllegalArgumentException("layer height must match configured worldHeight");
        }
        this.layers.add(layer);
        this.layers.sort(Comparator.comparingInt(LayeredWorld::getMinY));
    }
}
