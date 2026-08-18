package net.scape.project.supremepinata.statistics;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StatisticsStorage {
    CompletableFuture<Void> start();
    CompletableFuture<PlayerStats> load(UUID uuid);
    CompletableFuture<Void> increment(UUID uuid, String column, long amount);
    void shutdown();
}
