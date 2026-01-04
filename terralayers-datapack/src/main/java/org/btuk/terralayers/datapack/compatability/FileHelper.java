package org.btuk.terralayers.datapack.compatability;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper for getting datapack files for a specific datapack version.
 */
public class FileHelper {

    /**
     * Get the pack mc meta file for the specified datapack version.
     *
     * @param version the datapack version
     * @return the path to the pack mc meta file in the resources directory
     */
    public static URL getPackMCMeta(String version) {
        String resourcePath = "TerraLayers/pack.mcmeta." + version;
        URL resourceUrl = FileHelper.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
            return resourceUrl;
        } else {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
    }

    /**
     * Get the pack mc meta file for the specified datapack version.
     *
     * @param version the datapack version
     * @return the path to the pack mc meta file in the resources directory
     */
    public static URL getOverworldDimensionType(String version) {
        String resourcePath = "TerraLayers/dimension_type/overworld.json." + version;
        URL resourceUrl = FileHelper.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
            return resourceUrl;
        } else {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
    }
}
