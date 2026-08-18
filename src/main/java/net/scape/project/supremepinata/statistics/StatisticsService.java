package net.scape.project.supremepinata.statistics;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class StatisticsService {
    private final StatisticsStorage storage;
    private final Logger logger;

    public StatisticsService(StatisticsStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public CompletableFuture<PlayerStats> load(UUID uuid) {
        return storage.load(uuid);
    }

    public void addHit(UUID uuid) { increment(uuid, "total_hits", 1); }
    public void addParticipation(UUID uuid) { increment(uuid, "parties_participated", 1); }
    public void addWin(UUID uuid) { increment(uuid, "parties_won", 1); }
    public void addFinalHit(UUID uuid) { increment(uuid, "final_hits", 1); }
    public void addReward(UUID uuid) { increment(uuid, "rewards_won", 1); }

    private void increment(UUID uuid, String column, long amount) {
        storage.increment(uuid, column, amount).exceptionally(ex -> { logger.warning("Statistic update failed: " + ex.getMessage()); return null; });
    }

    public void flushAndShutdown() {
        storage.shutdown();
    }
}
