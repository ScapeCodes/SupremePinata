package net.scape.project.supremepinata.api;

import net.scape.project.supremepinata.pinata.ActivePinataEvent;
import net.scape.project.supremepinata.pinata.PinataType;
import net.scape.project.supremepinata.reward.RewardPool;
import net.scape.project.supremepinata.statistics.PlayerStats;
import org.bukkit.Location;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SupremePinataApi {
    Map<String, PinataType> pinataTypes();
    void registerPinataType(PinataType type);
    boolean spawnPinata(String id, Location location);
    void stopActiveEvent(String reason);
    Optional<ActivePinataEvent> activeEvent();
    void registerRewardPool(String pinataId, String pool, RewardPool rewards);
    CompletableFuture<PlayerStats> playerStats(UUID uuid);
}
