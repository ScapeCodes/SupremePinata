package net.scape.project.supremepinata.pinata;

import net.scape.project.supremepinata.reward.RewardPool;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PinataType(
        String id,
        String displayName,
        EntityType entityType,
        int requiredHits,
        int durationSeconds,
        double movementSpeed,
        double movementRadius,
        boolean movementEnabled,
        boolean knockback,
        boolean invulnerable,
        int hitCooldownMillis,
        int maxHitsPerPlayer,
        int minimumParticipation,
        double maxDistance,
        boolean projectileHits,
        boolean survivalOnlyHits,
        Cosmetics cosmetics,
        List<String> locations,
        BossBarSettings bossBar,
        HologramSettings hologram,
        Effects effects,
        Map<String, RewardPool> rewardPools
) {
    public RewardPool pool(String name) {
        return rewardPools.getOrDefault(name, new RewardPool());
    }

    public record Cosmetics(boolean glow, boolean rainbow, int rainbowIntervalTicks) {}
    public record BossBarSettings(boolean enabled, String title, boolean progress, String color, String overlay) {}
    public record HologramSettings(boolean enabled, double height, List<String> lines) {}
    public record Effects(Optional<Sound> spawnSound, Optional<Sound> hitSound, Optional<Sound> completeSound, String spawnParticle, String hitParticle, String completeParticle) {}
}
