package org.btuk.terralayers.datapack;

import org.btuk.terralayers.datapack.compatability.FileHelper;
import org.btuk.terralayers.datapack.compatability.PackVersion;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Java-representation of a Minecraft datapack.
 */
public class Datapack {

    private final Logger logger;

    private final String version;

    private final int yMin;
    private final int worldHeight;
    private final String name;

    /**
     * Creates a new datapack instance based on a datapack in the file system.
     *
     */
    public Datapack(Logger logger, String name, Path pathToDatapacks) {
        this.logger = logger;
        // Get the relevant variables from the filesystem.
        Path datapackPath = pathToDatapacks.resolve(name);
        if (!Files.exists(datapackPath)) {
            throw new IllegalArgumentException("Datapack does not exist at " + datapackPath);
        }
        Path packMcMeta = datapackPath.resolve("pack.mcmeta");
        Path packOverworld = datapackPath.resolve("data/minecraft/dimension_type/overworld.json");

        this.name = name;
        try {
            this.version = getPackVersion(packMcMeta);
            this.yMin = getMinY(packOverworld);
            this.worldHeight = getHeight(packOverworld);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read values from datapack", e);
        }
    }

    /**
     * Create a new datapack for the given Minecraft version.
     *
     * @param minecraftVersion the current server Minecraft version
     * @param yMin
     * @param worldHeight
     */
    public Datapack(Logger logger, String name, String minecraftVersion, int yMin, int worldHeight) {
        this.logger = logger;
        this.name = name;
        this.version = PackVersion.getPackVersion(minecraftVersion);
        this.yMin = yMin;
        this.worldHeight = worldHeight;
    }

    /**
     * Saves the datapack to the disk.
     *
     * @param pathToDatapacks the path to the datapack directory
     * @return true if the pack was saved successfully
     */
    public boolean saveToDisk(Path pathToDatapacks) {
        boolean success = true;

        // Check if the datapack already exists, if true, delete it.
        Path datapackPath = pathToDatapacks.resolve(name);
        if (Files.exists(datapackPath)) {
            success = deleteFromDisk(datapackPath);
        }

        try {
            Files.createDirectories(datapackPath);

            // Get the pack.mcmeta file for this version and save it datapack directory.
            Path packMcMetaTarget = datapackPath.resolve("pack.mcmeta");
            try (InputStream is = FileHelper.getPackMCMeta(version).openStream()) {
                Files.copy(is, packMcMetaTarget);
            }

            // Create the data/minecraft/dimension_type directory.
            Files.createDirectories(datapackPath.resolve("data/minecraft/dimension_type"));

            // Get the overworld.json dimension_type and replace the template values.
            Path targetPath = datapackPath.resolve("data/minecraft/dimension_type/overworld.json");
            try (InputStream is = FileHelper.getOverworldDimensionType(version).openStream()) {
                // Read the content of the template file
                String content = new String(is.readAllBytes());

                // Replace the placeholders for the yMin and worldHeight.
                content = content.replace("%yMin%", String.valueOf(yMin));
                content = content.replace("%worldHeight%", String.valueOf(worldHeight));

                // Write the modified content to the new file location.
                Files.writeString(targetPath, content);
            }

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

    private static String getPackVersion(Path path) throws IOException {
        String jsonContent = Files.readString(path);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        JsonNode minFormatNode = rootNode.path("pack").path("min_format");

        // Convert the array to a string with periods
        if (minFormatNode.isArray()) {
            return StreamSupport.stream(minFormatNode.spliterator(), false)
                    .map(node -> String.valueOf(node.asInt()))
                    .collect(Collectors.joining("."));
        } else {
            throw new IllegalArgumentException("min_format is not an array");
        }
    }

    private static int getHeight(Path path) throws IOException {
        String jsonContent = Files.readString(path);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonContent);

        return rootNode.path("height").asInt();
    }

    private static int getMinY(Path path) throws IOException {
        String jsonContent = Files.readString(path);

        // Parse the JSON using Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonContent);

        return rootNode.path("min_y").asInt();
    }

    public String getVersion() {
        return version;
    }

    public int getYMin() {
        return yMin;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public String getName() {
        return name;
    }
}
