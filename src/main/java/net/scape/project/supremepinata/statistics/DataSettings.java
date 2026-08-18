package net.scape.project.supremepinata.statistics;

public record DataSettings(
        boolean cacheData,
        StorageType type,
        String address,
        int port,
        String database,
        String username,
        String password,
        boolean useSsl,
        PoolSettings poolSettings
) {
    public enum StorageType {
        H2,
        SQLITE,
        MYSQL;

        public static StorageType parse(String input) {
            if (input == null || input.isBlank()) return SQLITE;
            try {
                return StorageType.valueOf(input.trim().replace("-", "_").toUpperCase());
            } catch (IllegalArgumentException ex) {
                return SQLITE;
            }
        }
    }

    public record PoolSettings(
            int minimumIdle,
            int maximumPoolSize,
            long idleTimeout,
            long connectionTimeout,
            long maxLifetime
    ) {}
}
