package net.scape.project.supremepinata.pinata;

import net.kyori.adventure.bossbar.BossBar;
import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.config.PinataRegistry;
import net.scape.project.supremepinata.event.PinataHitEvent;
import net.scape.project.supremepinata.event.PinataSpawnEvent;
import net.scape.project.supremepinata.location.LocationService;
import net.scape.project.supremepinata.reward.RewardService;
import net.scape.project.supremepinata.statistics.StatisticsService;
import net.scape.project.supremepinata.utility.SchedulerService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class PinataManager {
    public static NamespacedKey PINATA_KEY;
    private final JavaPlugin plugin;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final PinataRegistry registry;
    private final LocationService locations;
    private final RewardService rewards;
    private final StatisticsService statistics;
    private ActivePinataEvent active;

    public PinataManager(JavaPlugin plugin, SchedulerService scheduler, MessageService messages, PinataRegistry registry, LocationService locations, RewardService rewards, StatisticsService statistics) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.messages = messages;
        this.registry = registry;
        this.locations = locations;
        this.rewards = rewards;
        this.statistics = statistics;
        PINATA_KEY = new NamespacedKey(plugin, "pinata");
    }

    public Optional<ActivePinataEvent> active() {
        return Optional.ofNullable(active).filter(e -> !e.ended());
    }

    public boolean spawn(String id, Location explicit) {
        if (active().isPresent()) return false;
        PinataType type = registry.get(id).orElse(null);
        if (type == null) return false;
        Location location = explicit != null ? explicit : locations.random(type.locations()).orElse(null);
        if (location == null) location = Bukkit.getWorlds().get(0).getSpawnLocation();
        PinataSpawnEvent event = new PinataSpawnEvent(type, location);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        Location spawnLocation = location.clone();
        scheduler.runAtLocation(spawnLocation, () -> spawnNow(type, spawnLocation));
        return true;
    }

    private void spawnNow(PinataType type, Location location) {
        if (active().isPresent() || location.getWorld() == null) return;
        Entity entity = location.getWorld().spawnEntity(location, type.entityType());
        TextDisplay hologram = null;
        if (type.hologram().enabled()) {
            hologram = location.getWorld().spawn(location.clone().add(0, type.hologram().height(), 0), TextDisplay.class, display -> {
                display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                display.setSeeThrough(true);
                display.setShadowed(true);
                display.setTeleportDuration(2);
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(2);
            });
        }
        BossBar bar = null;
        if (type.bossBar().enabled()) {
            bar = BossBar.bossBar(net.kyori.adventure.text.Component.empty(), 0f, parseColor(type.bossBar().color()), parseOverlay(type.bossBar().overlay()));
        }
        this.active = new ActivePinataEvent(plugin, scheduler, messages, rewards, statistics, type, location, entity, hologram, bar);
        active.start();
    }

    public boolean handleHit(Player player, Entity entity) {
        ActivePinataEvent event = active().orElse(null);
        if (event == null || !event.entity().getUniqueId().equals(entity.getUniqueId())) return false;
        PinataHitEvent hitEvent = new PinataHitEvent(event, player);
        Bukkit.getPluginManager().callEvent(hitEvent);
        if (hitEvent.isCancelled()) return false;
        return event.hit(player);
    }

    public void stopActiveEvent(String reason) {
        active().ifPresent(event -> event.end(reason));
        active = null;
    }

    public void shutdown() {
        stopActiveEvent("shutdown");
    }

    private BossBar.Color parseColor(String value) {
        try { return BossBar.Color.valueOf(value.toUpperCase()); } catch (IllegalArgumentException ex) { return BossBar.Color.YELLOW; }
    }

    private BossBar.Overlay parseOverlay(String value) {
        try { return BossBar.Overlay.valueOf(value.toUpperCase()); } catch (IllegalArgumentException ex) { return BossBar.Overlay.PROGRESS; }
    }
}
