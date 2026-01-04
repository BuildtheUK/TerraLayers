package org.btuk.terralayers.plugin.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Handles loading, versioning and safe auto-updating of the plugin configuration.
 * <p>
 * Strategy:
 * - The default config included in the JAR contains a key "config-version".
 * - On load, compare the on-disk version with the bundled version.
 * - If the bundled version is newer, create a timestamped backup and merge in any
 *   new default keys while preserving user values. Then set the new version and save.
 */
public final class ConfigManager {

    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String CONFIG_VERSION_KEY = "config-version";

    private final JavaPlugin plugin;
    private final Logger logger;

    private final File configFile;
    private FileConfiguration config;
    private FileConfiguration defaults;

    private int currentVersion;
    private int bundledVersion;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
    }

    public void load() {
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        // If no config yet, copy the default file
        if (!configFile.exists()) {
            plugin.saveResource(CONFIG_FILE_NAME, false);
        }

        // Load user config from disk
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // Load defaults from JAR
        try (InputStream in = plugin.getResource(CONFIG_FILE_NAME)) {
            if (in != null) {
                this.defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            } else {
                this.defaults = new YamlConfiguration();
            }
        } catch (Exception e) {
            this.defaults = new YamlConfiguration();
            logger.warning("Failed to load default config from JAR: " + e.getMessage());
        }

        // Apply defaults for missing values when reading
        config.setDefaults(defaults);
        ((YamlConfiguration) config).options().copyDefaults(true);

        this.currentVersion = config.getInt(CONFIG_VERSION_KEY, 0);
        this.bundledVersion = defaults.getInt(CONFIG_VERSION_KEY, Math.max(1, currentVersion));

        if (currentVersion < bundledVersion) {
            // Perform update/merge
            backupConfig();
            mergeDefaults(config, defaults);
            config.set(CONFIG_VERSION_KEY, bundledVersion);
            save();
            logger.info("Config updated from v" + currentVersion + " to v" + bundledVersion + " (backup created).");
        } else if (currentVersion > bundledVersion) {
            logger.warning("Your config-version (" + currentVersion + ") is newer than the plugin's bundled version (" + bundledVersion + "). " +
                    "Proceeding but you may be running a newer config than this build expects.");
        }
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            logger.severe("Failed to save configuration: " + e.getMessage());
        }
    }

    private void backupConfig() {
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File backup = new File(configFile.getParentFile(), CONFIG_FILE_NAME + ".bak-" + stamp);
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Created config backup: " + backup.getName());
        } catch (Exception e) {
            logger.warning("Failed to create config backup: " + e.getMessage());
        }
    }

    private static void mergeDefaults(FileConfiguration target, FileConfiguration defaults) {
        Set<String> keys = defaults.getKeys(true);
        for (String key : keys) {
            if (defaults.isConfigurationSection(key)) {
                // sections are implicit; nothing to set
                continue;
            }
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getBundledVersion() {
        return bundledVersion;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public int getWorldHeight() {
        return config.getInt("worldHeight", defaults.getInt("worldHeight", 1024));
    }

    public int getBufferSize() {
        return config.getInt("bufferSize", defaults.getInt("bufferSize", 256));
    }

    public int getGlobalMin() {
        return config.getInt("globalMin", defaults.getInt("globalMin", -11264));
    }

    public int getGlobalMax() {
        return config.getInt("globalMax", defaults.getInt("globalMax", 9216));
    }

    public String getWorldBaseName() {
        return config.getString("worldBaseName", defaults.getString("worldBaseName", "earth"));
    }
}
