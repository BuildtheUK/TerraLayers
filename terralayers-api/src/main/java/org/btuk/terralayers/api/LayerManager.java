package org.btuk.terralayers.api;

import org.bukkit.World;

import java.util.List;
import java.util.Optional;

/**
 * Provides information about the configured layered worlds and basic queries.
 */
public interface LayerManager {
    /**
     * All configured layers in ascending order by global Y minimum.
     */
    List<LayeredWorld> getLayers();

    /**
     * Finds the layer that covers the provided global Y coordinate.
     *
     * @param globalY the y-level to get the layer for
     */
    Optional<LayeredWorld> getLayerForGlobalY(int globalY);

    /**
     * Finds the layer that matches the given Bukkit world.
     *
     * @param world the Bukkit world
     */
    Optional<LayeredWorld> getLayerForWorld(World world);

    /**
     * The configured world height per layer.
     */
    int getWorldHeight();

    /**
     * The configured buffer size where layers overlap.
     */
    int getBufferSize();
}
