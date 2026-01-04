package org.btuk.terralayers.datapack;

import org.btuk.terralayers.datapack.compatability.FileHelper;
import org.btuk.terralayers.datapack.compatability.PackVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * Java-representation of a Minecraft datapack.
 */
public class Datapack {

    private final Logger logger;

    private final String version;

    private final int yMin;
    private final int worldHeight;

    /**
     * Creates a new datapack instance based on a datapack in the file system.
     *
     * @param relativePathFromServer the path to the datapack from the server jar
     */
    public Datapack(Logger logger, Path relativePathFromServer) {
        this.logger = logger;
        // Get the relevant variables from the filesystem.
        this.version = null;
        this.yMin = 0;
        this.worldHeight = 256;
    }

    /**
     * Create a new datapack for the given Minecraft version.
     *
     * @param minecraftVersion the current server Minecraft version
     * @param yMin
     * @param worldHeight
     */
    public Datapack(Logger logger, String minecraftVersion, int yMin, int worldHeight) {
        this.logger = logger;
        this.version = PackVersion.getPackVersion(minecraftVersion);
        this.yMin = yMin;
        this.worldHeight = worldHeight;
    }

    /**
     * Saves the datapack to the disk.
     *
     * @param relativePathFromServer the path to the datapack from the server jar
     * @return true if the pack was saved successfully
     */
    public boolean saveToDisk(Path relativePathFromServer) {
        boolean success = true;

        // Check if the datapack already exists, if true, delete it.
        if (Files.exists(relativePathFromServer)) {
            success = deleteFromDisk(relativePathFromServer);
        }

        try {
            Files.createDirectory(relativePathFromServer);

            // Get the pack.mcmeta file for this version and save it datapack directory.
            Path packMcMeta = FileHelper.getPackMCMeta(version);
            Files.copy(packMcMeta, relativePathFromServer.resolve("pack.mcmeta"));

            // Create the data/minecraft/dimension_type directory.
            Files.createDirectories(relativePathFromServer.resolve("data/minecraft/dimension_type"));

            // Get the overworld.json dimension_type and replace the template values.
            Path overworldDimensionType = FileHelper.getOverworldDimensionType(version);

            // Read the content of the template file
            String content = Files.readString(overworldDimensionType);

            // Replace the placeholders for the minY and worldHeight.
            content = content.replace("%minY%", String.valueOf(yMin));
            content = content.replace("%worldHeight%", String.valueOf(worldHeight));

            // Write the modified content to the new file location.
            Path targetPath = relativePathFromServer.resolve("data/minecraft/dimension_type/overworld.json");
            Files.writeString(targetPath, content);

        } catch (IOException e) {
            logger.severe("Failed to save datapack to disk: " + e.getMessage());
            success = false;
        }

        return success;
    }

    private boolean deleteFromDisk(Path relativePathFromServer) {
        try {
            try (var stream = Files.walk(relativePathFromServer)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.severe("Failed to delete file: " + path);
                    }
                });
            }
        } catch (IOException e) {
            logger.severe("Failed to delete directory: " + relativePathFromServer);
            return false;
        }

        return !Files.exists(relativePathFromServer);
    }

    public String getVersion() {
        return version;
    }

}
