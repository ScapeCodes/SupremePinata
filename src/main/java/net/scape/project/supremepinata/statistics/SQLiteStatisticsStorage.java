package net.scape.project.supremepinata.statistics;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SQLiteStatisticsStorage implements StatisticsStorage {
    private static final Set<String> COLUMNS = Set.of("total_hits", "parties_participated", "parties_won", "final_hits", "rewards_won");
    private final JavaPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "SupremePinata-SQLite"));
    private Connection connection;

    public SQLiteStatisticsStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                File data = new File(plugin.getDataFolder(), "data");
                if (!data.exists() && !data.mkdirs()) plugin.getLogger().warning("Could not create data folder.");
                connection = DriverManager.getConnection("jdbc:sqlite:" + new File(data, "statistics.db"));
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS player_statistics (uuid TEXT PRIMARY KEY, total_hits INTEGER NOT NULL DEFAULT 0, parties_participated INTEGER NOT NULL DEFAULT 0, parties_won INTEGER NOT NULL DEFAULT 0, final_hits INTEGER NOT NULL DEFAULT 0, rewards_won INTEGER NOT NULL DEFAULT 0)");
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Could not initialize SQLite", ex);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<PlayerStats> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensure(uuid);
                try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_statistics WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return new PlayerStats(uuid, rs.getLong("total_hits"), rs.getLong("parties_participated"), rs.getLong("parties_won"), rs.getLong("final_hits"), rs.getLong("rewards_won"));
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Could not load stats for " + uuid + ": " + ex.getMessage());
            }
            return PlayerStats.empty(uuid);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> increment(UUID uuid, String column, long amount) {
        if (!COLUMNS.contains(column)) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid stat column " + column));
        return CompletableFuture.runAsync(() -> {
            try {
                ensure(uuid);
                try (PreparedStatement ps = connection.prepareStatement("UPDATE player_statistics SET " + column + "=" + column + "+? WHERE uuid=?")) {
                    ps.setLong(1, amount);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning("Could not update stats for " + uuid + ": " + ex.getMessage());
            }
        }, executor);
    }

    @Override
    public void shutdown() {
        executor.submit(() -> { try { if (connection != null) connection.close(); } catch (SQLException ignored) {} });
        executor.shutdown();
    }

    private void ensure(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT OR IGNORE INTO player_statistics(uuid) VALUES(?)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }
}
