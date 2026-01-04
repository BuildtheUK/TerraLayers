package org.btuk.terralayers.api;

import org.bukkit.World;

/**
 * Read-only description of a single layered world in the TerraLayers system.
 */
public interface LayeredWorld {
    /**
     * Bukkit world backing this layer.
     */
    World getWorld();

    /**
     * Name of this layer/world.
     */
    String getName();

    /**
     * Y minimum (inclusive) covered by this layer.
     */
    int getMinY();

    /**
     * Y maximum (exclusive) covered by this layer.
     */
    int getMaxY();

    /**
     * Buffer size (in blocks) overlapping with adjacent layers.
     * The teleportation point is half-way through the buffer.
     */
    int getBufferSize();

    /**
     * The point at which the player should be teleported to the next layer when moving in the negative-Y direction.
     *
     * @return the y-level from which the player should be teleported
     */
    default int getTeleportMinY() {
        return getMinY() - (getBufferSize() / 2);
    }

    /**
     * The point at which the player should be teleported to the next layer when moving in the positive-Y direction.
     *
     * @return the y-level from which the player should be teleported
     */
    default int getTeleportMaxY() {
        return getMaxY() + (getBufferSize() / 2);
    }
}
