package org.btuk.terralayers.datapack;

import org.bukkit.Bukkit;

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
        return new Datapack(logger, DATAPACK_NAME, minecraftVersion, yMin, worldHeight);
    }

    public Datapack loadDatapack(Path worldPath) {
        return new Datapack(logger, DATAPACK_NAME, worldPath.resolve(DATAPACK_DIRECTORY));
    }

    /**
     * Migrate the datapack to support the current Minecraft version.
     *
     * @param datapack the datapack
     * @return true if the datapack was successfully migrated
     */
    public boolean migrateDatapack(Datapack datapack) {
        // We currently only support 1.21.11.
        return true;
    }

    /**
     * Saves the datapack to the given world in the file system.
     *
     * @param datapack  the datapack
     * @param worldName the world name
     * @return whether the datapack was successfully saved
     */
    public boolean saveDatapackToWorld(Datapack datapack, Path serverPath, String worldName) {
        return datapack.saveToDisk(serverPath.resolve(worldName).resolve(DATAPACK_DIRECTORY));
    }

    /**
     * Reloads the datapack on the server.
     *
     * @param logger logger
     * @return true if the datapack was reloaded
     */
    public boolean enableDatapack(Logger logger, Datapack datapack) {
        io.papermc.paper.datapack.DatapackManager manager = Bukkit.getDatapackManager();
        manager.refreshPacks();
        io.papermc.paper.datapack.Datapack pack = manager.getPack("file/" + datapack.getName());
        if (pack == null) {
            logger.severe("Failed to reload datapack: " + datapack.getName());
            return false;
        }
        pack.setEnabled(true);
        return true;
    }
}
