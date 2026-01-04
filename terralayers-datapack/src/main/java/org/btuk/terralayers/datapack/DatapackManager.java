package org.btuk.terralayers.datapack;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Class to manage datapacks for layered worlds. Used to create initial datapacks or migrate existing ones to newer versions.
 */
public class DatapackManager {

    private static final String DATAPACK_NAME = "TerraLayers";
    private static final String DATAPACK_DIRECTORY = "datapacks";

    private final Logger logger;
    private final int yMin;
    private final int worldHeight;

    /**
     * Constructor
     */
    public DatapackManager(Logger logger, int yMin, int worldHeight) {
        this.logger = logger;
        this.yMin = yMin;
        this.worldHeight = worldHeight;
    }

    public Datapack createDatapack(String minecraftVersion) {
        return new Datapack(logger, minecraftVersion, yMin, worldHeight);
    }

    /**
     * Migrate the datapack to support the current Minecraft version.
     *
     * @param datapack the datapack
     * @return true if the datapack was successfully migrated
     */
    public boolean migrateDatapack(Datapack datapack) {

    }

    /**
     * Saves the datapack to the given world in the file system.
     *
     * @param datapack  the datapack
     * @param worldName the world name
     * @return whether the datapack was successfully saved
     */
    public boolean saveDatapackToWorld(Datapack datapack, Path serverPath, String worldName) {
        return datapack.saveToDisk(serverPath.resolve(worldName).resolve(DATAPACK_DIRECTORY).resolve(DATAPACK_NAME));
    }

    /**
     * Reloads the datapack on the server.
     *
     * @param datapack the datapack to reload
     * @return true if the datapack was reloaded
     */
    public boolean reloadDatapack(Datapack datapack) {

    }
}
