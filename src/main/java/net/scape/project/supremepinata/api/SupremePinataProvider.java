package net.scape.project.supremepinata.api;

import net.scape.project.supremepinata.config.PinataRegistry;
import net.scape.project.supremepinata.pinata.ActivePinataEvent;
import net.scape.project.supremepinata.pinata.PinataManager;
import net.scape.project.supremepinata.pinata.PinataType;
import net.scape.project.supremepinata.reward.RewardPool;
import net.scape.project.supremepinata.reward.RewardService;
import net.scape.project.supremepinata.statistics.PlayerStats;
import net.scape.project.supremepinata.statistics.StatisticsService;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SupremePinataProvider {
    private static SupremePinataApi api;
    private SupremePinataProvider() {}

    public static void set(Plugin plugin, PinataManager manager, PinataRegistry registry, RewardService rewards, StatisticsService stats) {
        api = new SupremePinataApi() {
            @Override public Map<String, PinataType> pinataTypes() { return registry.types(); }
            @Override public void registerPinataType(PinataType type) { registry.register(type); }
            @Override public boolean spawnPinata(String id, Location location) { return manager.spawn(id, location); }
            @Override public void stopActiveEvent(String reason) { manager.stopActiveEvent(reason); }
            @Override public Optional<ActivePinataEvent> activeEvent() { return manager.active(); }
            @Override public void registerRewardPool(String pinataId, String pool, RewardPool rewardPool) { registry.get(pinataId).ifPresent(type -> type.rewardPools().put(pool, rewardPool)); }
            @Override public CompletableFuture<PlayerStats> playerStats(UUID uuid) { return stats.load(uuid); }
        };
    }

    public static SupremePinataApi api() {
        if (api == null) throw new IllegalStateException("SupremePinata API is not available yet");
        return api;
    }

    public static void clear() { api = null; }
}
