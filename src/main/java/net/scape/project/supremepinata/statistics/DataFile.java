package net.scape.project.supremepinata.statistics;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class DataFile {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public DataFile(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void reload() {
        if (!file.exists()) plugin.saveResource("data.yml", false);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public DataSettings settings() {
        if (config == null) reload();
        return new DataSettings(
                config.getBoolean("data.cache-data", true),
                DataSettings.StorageType.parse(config.getString("data.type", "SQLite")),
                config.getString("data.address", "host"),
                config.getInt("data.port", 3306),
                config.getString("data.database", "database"),
                config.getString("data.username", "user"),
                config.getString("data.password", "pass"),
                config.getBoolean("data.useSSL", false),
                new DataSettings.PoolSettings(
                        config.getInt("data.mysql-pool-settings.minimum-idle", 10),
                        config.getInt("data.mysql-pool-settings.maximum-pool-size", 20),
                        config.getLong("data.mysql-pool-settings.timeouts.idle", 870000000L),
                        config.getLong("data.mysql-pool-settings.timeouts.connection", 870000000L),
                        config.getLong("data.mysql-pool-settings.timeouts.max-lifetime", 870000000L)
                )
        );
    }
}
