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
    public static Path getPackMCMeta(String version) {
        try {
            String resourcePath = "TerraLayers/pack.mcmeta." + version;
            URL resourceUrl = FileHelper.class.getClassLoader().getResource(resourcePath);
            if (resourceUrl != null) {
                return Paths.get(resourceUrl.toURI());
            } else {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get path for pack.mcmeta version " + version, e);
        }
    }

    /**
     * Get the pack mc meta file for the specified datapack version.
     *
     * @param version the datapack version
     * @return the path to the pack mc meta file in the resources directory
     */
    public static Path getOverworldDimensionType(String version) {
        try {
            String resourcePath = "TerraLayers/dimension_type." + version;
            URL resourceUrl = FileHelper.class.getClassLoader().getResource(resourcePath);
            if (resourceUrl != null) {
                return Paths.get(resourceUrl.toURI());
            } else {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get path for dimension_type/overworld.json version " + version, e);
        }
    }
}
