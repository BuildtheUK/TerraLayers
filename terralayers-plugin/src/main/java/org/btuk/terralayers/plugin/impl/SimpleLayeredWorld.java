package org.btuk.terralayers.plugin.impl;

import org.btuk.terralayers.api.LayeredWorld;
import org.bukkit.World;

/**
 * Layered world backed by a Bukkit world.
 */
public class SimpleLayeredWorld implements LayeredWorld {
    private final World world;
    private final String name;
    private final int minY;
    private final int maxY;
    private final int bufferSize;

    public SimpleLayeredWorld(World world, int minY, int maxY, int bufferSize) {
        this.world = world;
        this.name = world.getName();
        this.minY = minY;
        this.maxY = maxY;
        this.bufferSize = bufferSize;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }
}
