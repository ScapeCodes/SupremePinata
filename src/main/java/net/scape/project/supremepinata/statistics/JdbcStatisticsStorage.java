package net.scape.project.supremepinata.statistics;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class JdbcStatisticsStorage implements StatisticsStorage {
    private static final Set<String> COLUMNS = Set.of("total_hits", "parties_participated", "parties_won", "final_hits", "rewards_won");

    private final JavaPlugin plugin;
    private final DataSettings settings;
    private final ExecutorService executor;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private Connection connection;

    public JdbcStatisticsStorage(JavaPlugin plugin, DataSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.executor = Executors.newFixedThreadPool(threadCount(settings), r -> new Thread(r, "SupremePinata-" + settings.type()));
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                connection = DriverManager.getConnection(jdbcUrl(), settings.username(), settings.password());
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS player_statistics (uuid VARCHAR(36) PRIMARY KEY, total_hits BIGINT NOT NULL DEFAULT 0, parties_participated BIGINT NOT NULL DEFAULT 0, parties_won BIGINT NOT NULL DEFAULT 0, final_hits BIGINT NOT NULL DEFAULT 0, rewards_won BIGINT NOT NULL DEFAULT 0)");
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Could not initialize " + settings.type() + " statistics storage", ex);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<PlayerStats> load(UUID uuid) {
        PlayerStats cached = cache.get(uuid);
        if (settings.cacheData() && cached != null) return CompletableFuture.completedFuture(cached);
        return CompletableFuture.supplyAsync(() -> loadNow(uuid), executor);
    }

    @Override
    public CompletableFuture<Void> increment(UUID uuid, String column, long amount) {
        if (!COLUMNS.contains(column)) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid stat column " + column));
        return CompletableFuture.runAsync(() -> incrementNow(uuid, column, amount), executor);
    }

    @Override
    public void shutdown() {
        executor.submit(() -> {
            try {
                if (connection != null) connection.close();
            } catch (SQLException ignored) {
            }
        });
        executor.shutdown();
    }

    private PlayerStats loadNow(UUID uuid) {
        try {
            ensure(uuid);
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_statistics WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PlayerStats stats = new PlayerStats(uuid, rs.getLong("total_hits"), rs.getLong("parties_participated"), rs.getLong("parties_won"), rs.getLong("final_hits"), rs.getLong("rewards_won"));
                        if (settings.cacheData()) cache.put(uuid, stats);
                        return stats;
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not load stats for " + uuid + ": " + ex.getMessage());
        }
        return PlayerStats.empty(uuid);
    }

    private void incrementNow(UUID uuid, String column, long amount) {
        try {
            ensure(uuid);
            try (PreparedStatement ps = connection.prepareStatement("UPDATE player_statistics SET " + column + "=" + column + "+? WHERE uuid=?")) {
                ps.setLong(1, amount);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
            if (settings.cacheData()) cache.put(uuid, loadNow(uuid));
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not update stats for " + uuid + ": " + ex.getMessage());
        }
    }

    private void ensure(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(insertIgnoreSql())) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    private String jdbcUrl() {
        return switch (settings.type()) {
            case H2 -> "jdbc:h2:" + dataFile("statistics").getPath();
            case SQLITE -> "jdbc:sqlite:" + dataFile("statistics.db").getPath();
            case MYSQL -> "jdbc:mysql://" + settings.address() + ':' + settings.port() + '/' + settings.database() + "?useSSL=" + settings.useSsl();
        };
    }

    private File dataFile(String name) {
        File data = new File(plugin.getDataFolder(), "data");
        if (!data.exists() && !data.mkdirs()) plugin.getLogger().warning("Could not create data folder.");
        return new File(data, name);
    }

    private String insertIgnoreSql() {
        return switch (settings.type()) {
            case MYSQL -> "INSERT IGNORE INTO player_statistics(uuid) VALUES(?)";
            case SQLITE -> "INSERT OR IGNORE INTO player_statistics(uuid) VALUES(?)";
            case H2 -> "MERGE INTO player_statistics(uuid) KEY(uuid) VALUES(?)";
        };
    }

    private static int threadCount(DataSettings settings) {
        if (settings.type() != DataSettings.StorageType.MYSQL || !settings.cacheData()) return 1;
        return Math.max(1, settings.poolSettings().maximumPoolSize());
    }
}
