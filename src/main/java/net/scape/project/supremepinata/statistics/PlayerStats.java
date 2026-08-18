package net.scape.project.supremepinata.statistics;

import java.util.UUID;

public record PlayerStats(UUID uuid, long totalHits, long partiesParticipated, long partiesWon, long finalHits, long rewardsWon) {
    public static PlayerStats empty(UUID uuid) {
        return new PlayerStats(uuid, 0, 0, 0, 0, 0);
    }
}
