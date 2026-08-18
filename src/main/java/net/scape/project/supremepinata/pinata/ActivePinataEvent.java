package net.scape.project.supremepinata.pinata;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.event.PinataCompleteEvent;
import net.scape.project.supremepinata.event.PinataEndEvent;
import net.scape.project.supremepinata.event.PinataRewardEvent;
import net.scape.project.supremepinata.reward.RewardService;
import net.scape.project.supremepinata.statistics.StatisticsService;
import net.scape.project.supremepinata.utility.SchedulerService;
import net.scape.project.supremepinata.utility.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActivePinataEvent {
    private static final double SAFE_PINATA_HEALTH = 1024.0D;
    private static final ChatColor[] RAINBOW_GLOW_COLORS = {
            ChatColor.RED,
            ChatColor.GOLD,
            ChatColor.YELLOW,
            ChatColor.GREEN,
            ChatColor.AQUA,
            ChatColor.BLUE,
            ChatColor.LIGHT_PURPLE
    };

    private final JavaPlugin plugin;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final RewardService rewards;
    private final StatisticsService statistics;
    private final PinataType type;
    private final Location origin;
    private final Entity entity;
    private final TextDisplay hologram;
    private final BossBar bossBar;
    private final Instant started = Instant.now();
    private final Map<UUID, Integer> hits = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private Object task;
    private Location movementTarget;
    private long lastViewTextUpdate;
    private Object[] rainbowColors;
    private Method rainbowColorSetter;
    private Team rainbowGlowTeam;
    private int rainbowIndex;
    private int totalHits;
    private boolean ended;

    public ActivePinataEvent(JavaPlugin plugin, SchedulerService scheduler, MessageService messages, RewardService rewards, StatisticsService statistics, PinataType type, Location origin, Entity entity, TextDisplay hologram, BossBar bossBar) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.messages = messages;
        this.rewards = rewards;
        this.statistics = statistics;
        this.type = type;
        this.origin = origin.clone();
        this.entity = entity;
        this.hologram = hologram;
        this.bossBar = bossBar;
    }

    public void start() {
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setRemoveWhenFarAway(false);
            living.setCanPickupItems(false);
            living.setMaxHealth(SAFE_PINATA_HEALTH);
            living.setHealth(Math.min(SAFE_PINATA_HEALTH, living.getMaxHealth()));
            living.setInvulnerable(false);
        }
        entity.setGlowing(type.cosmetics().glow() || type.cosmetics().rainbow());
        prepareRainbowCosmetic();
        entity.customName(Text.parse(type.displayName(), Map.of()));
        entity.setCustomNameVisible(true);
        entity.getPersistentDataContainer().set(PinataManager.PINATA_KEY, PersistentDataType.STRING, type.id());
        updateViews();
        task = scheduler.runEntityRepeating(entity, this::tick, 1L, 1L);
        scheduler.runGlobal(() -> Bukkit.broadcast(messages.component("event-started", placeholders(null))));
        play(type.effects().spawnSound());
        particle(type.effects().spawnParticle(), 30);
    }

    public boolean hit(Player player) {
        if (ended || !entity.isValid()) return false;
        if (type.survivalOnlyHits() && player.getGameMode() != GameMode.SURVIVAL) {
            messages.send(player, "survival-only-hit");
            return false;
        }
        if (!player.getWorld().equals(entity.getWorld()) || player.getLocation().distanceSquared(entity.getLocation()) > type.maxDistance() * type.maxDistance()) return false;
        long now = System.currentTimeMillis();
        long next = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < next) return false;
        int current = hits.getOrDefault(player.getUniqueId(), 0);
        if (type.maxHitsPerPlayer() > -1 && current >= type.maxHitsPerPlayer()) return false;
        cooldowns.put(player.getUniqueId(), now + type.hitCooldownMillis());
        hits.put(player.getUniqueId(), current + 1);
        names.put(player.getUniqueId(), player.getName());
        totalHits++;
        statistics.addHit(player.getUniqueId());
        roll(player, "hit");
        play(type.effects().hitSound());
        particle(type.effects().hitParticle(), 4);
        updateViews();
        if (totalHits >= type.requiredHits()) complete(player);
        return true;
    }

    public void complete(Player finalHitter) {
        if (ended) return;
        Bukkit.getPluginManager().callEvent(new PinataCompleteEvent(this, finalHitter));
        List<PinataContribution> contributions = leaderboard();
        if (finalHitter != null) {
            statistics.addFinalHit(finalHitter.getUniqueId());
            roll(finalHitter, "final-hit");
        }
        contributions.forEach(c -> {
            statistics.addParticipation(c.uuid());
            Player p = Bukkit.getPlayer(c.uuid());
            if (p != null && c.hits() >= type.minimumParticipation()) roll(p, "participation");
        });
        contributions.stream().findFirst().ifPresent(winner -> statistics.addWin(winner.uuid()));
        for (int i = 0; i < contributions.size(); i++) {
            Player p = Bukkit.getPlayer(contributions.get(i).uuid());
            if (p != null) roll(p, "top-" + (i + 1));
        }
        scheduler.runGlobal(() -> Bukkit.broadcast(messages.component("event-complete", placeholders(finalHitter))));
        play(type.effects().completeSound());
        particle(type.effects().completeParticle(), 80);
        end("complete");
    }

    public void end(String reason) {
        if (ended) return;
        ended = true;
        scheduler.cancel(task);
        if (bossBar != null) scheduler.runGlobal(() -> Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bossBar)));
        if (hologram != null) hologram.remove();
        cleanupRainbowCosmetic();
        if (entity.isValid()) entity.remove();
        scheduler.runGlobal(() -> Bukkit.getPluginManager().callEvent(new PinataEndEvent(this, reason)));
    }

    private void tick() {
        if (!entity.isValid()) { end("entity-removed"); return; }
        if (Duration.between(started, Instant.now()).getSeconds() >= type.durationSeconds()) { end("timeout"); return; }
        if (type.movementEnabled()) move();
        updateRainbowCosmetic();
        updateViews();
    }

    private void prepareRainbowCosmetic() {
        if (!type.cosmetics().rainbow()) return;
        prepareRainbowGlowTeam();
        for (Method method : entity.getClass().getMethods()) {
            if (!method.getName().equals("setColor") || method.getParameterCount() != 1 || !method.getParameterTypes()[0].isEnum()) continue;
            Object[] colors = method.getParameterTypes()[0].getEnumConstants();
            if (colors == null || colors.length == 0) continue;
            rainbowColorSetter = method;
            rainbowColors = colors;
            updateRainbowCosmetic();
            return;
        }
        if (rainbowGlowTeam == null) plugin.getLogger().warning("Pinata type '" + type.id() + "' has entity.rainbow enabled, but " + entity.getType() + " does not support setColor or colored glow.");
    }

    private void prepareRainbowGlowTeam() {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String name = ("sp_" + entity.getUniqueId().toString().replace("-", "")).substring(0, 16);
            Team existing = scoreboard.getTeam(name);
            if (existing != null) existing.unregister();
            rainbowGlowTeam = scoreboard.registerNewTeam(name);
            rainbowGlowTeam.addEntry(entity.getUniqueId().toString());
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("Could not create rainbow glow team for pinata type '" + type.id() + "': " + ex.getMessage());
        }
    }

    private void updateRainbowCosmetic() {
        if (rainbowGlowTeam == null && (rainbowColorSetter == null || rainbowColors == null || rainbowColors.length == 0)) return;
        if (type.cosmetics().rainbowIntervalTicks() > 1 && totalRuntimeTicks() % type.cosmetics().rainbowIntervalTicks() != 0) return;
        try {
            if (rainbowGlowTeam != null) rainbowGlowTeam.setColor(RAINBOW_GLOW_COLORS[rainbowIndex % RAINBOW_GLOW_COLORS.length]);
            if (rainbowColorSetter != null && rainbowColors != null && rainbowColors.length > 0) rainbowColorSetter.invoke(entity, rainbowColors[rainbowIndex % rainbowColors.length]);
            rainbowIndex++;
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            rainbowColorSetter = null;
            rainbowColors = null;
            plugin.getLogger().warning("Disabled entity body color cycling for pinata type '" + type.id() + "': " + ex.getMessage());
        }
    }

    private void cleanupRainbowCosmetic() {
        if (rainbowGlowTeam == null) return;
        try {
            rainbowGlowTeam.removeEntry(entity.getUniqueId().toString());
            rainbowGlowTeam.unregister();
        } catch (IllegalStateException ignored) {
        } finally {
            rainbowGlowTeam = null;
        }
    }

    private long totalRuntimeTicks() {
        return Math.max(0L, Duration.between(started, Instant.now()).toMillis() / 50L);
    }

    private void move() {
        Location current = entity.getLocation();
        if (resetToOriginIfWorldChanged(current)) return;

        double radius = Math.max(1.0D, type.movementRadius());
        if (resetToOriginIfOutsideLeash(current, radius)) return;

        if (movementTarget == null || horizontalDistanceSquared(current, movementTarget) < 1.0D || horizontalDistanceSquared(movementTarget, origin) > radius * radius) {
            movementTarget = randomTarget(radius);
        }

        Vector direction = movementTarget.toVector().subtract(current.toVector());
        direction.setY(0);
        if (direction.lengthSquared() < 0.01D) {
            movementTarget = randomTarget(radius);
            return;
        }

        double panicSpeed = Math.min(1.15D, Math.max(0.22D, 0.34D * type.movementSpeed()));
        Vector movement = direction.normalize().multiply(panicSpeed);
        Vector sideways = new Vector(-movement.getZ(), 0.0D, movement.getX()).normalize().multiply(Math.sin(System.currentTimeMillis() / 90.0D) * 0.055D * type.movementSpeed());
        movement.add(sideways);
        Location next = current.clone().add(movement);
        if (horizontalDistanceSquared(next, origin) > radius * radius) {
            movementTarget = randomTarget(radius);
            Vector pullBack = origin.toVector().subtract(current.toVector()).setY(0);
            if (pullBack.lengthSquared() > 0.01D) next = current.clone().add(pullBack.normalize().multiply(panicSpeed));
        }
        next.setY(current.getY());
        next.setYaw((float) Math.toDegrees(Math.atan2(-movement.getX(), movement.getZ())));
        next.setPitch(current.getPitch());
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getPathfinder().moveTo(next, Math.max(1.25D, type.movementSpeed() * 1.65D));
        }
        entity.teleport(next);
        entity.setVelocity(movement.multiply(1.15D).setY(0.0D));
    }

    private boolean resetToOriginIfWorldChanged(Location current) {
        if (current.getWorld().equals(origin.getWorld())) return false;
        entity.teleport(origin);
        movementTarget = null;
        return true;
    }

    private boolean resetToOriginIfOutsideLeash(Location current, double radius) {
        double leashRadius = radius + 2.0D;
        if (horizontalDistanceSquared(current, origin) <= leashRadius * leashRadius) return false;
        entity.teleport(origin);
        movementTarget = null;
        return true;
    }

    private Location randomTarget(double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0.0D, Math.PI * 2.0D);
        double distance = random.nextDouble(radius * 0.35D, radius);
        return origin.clone().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
    }

    private double horizontalDistanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private void updateViews() {
        Map<String, String> ph = placeholders(null);
        long now = System.currentTimeMillis();
        boolean updateText = now - lastViewTextUpdate >= 250L;
        if (bossBar != null) {
            if (updateText) bossBar.name(Text.parse(type.bossBar().title(), ph));
            if (type.bossBar().progress()) bossBar.progress(Math.max(0.0f, Math.min(1.0f, (float) totalHits / type.requiredHits())));
            scheduler.runGlobal(() -> Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(bossBar)));
        }
        if (hologram != null) {
            if (updateText) {
                List<Component> lines = type.hologram().lines().stream().map(line -> Text.parse(line, ph)).toList();
                hologram.text(Component.join(net.kyori.adventure.text.JoinConfiguration.separator(Component.newline()), lines));
            }
            hologram.teleport(entity.getLocation().clone().add(0, type.hologram().height(), 0));
        }
        if (updateText) lastViewTextUpdate = now;
    }

    private void roll(Player player, String pool) {
        type.pool(pool).roll().ifPresent(reward -> {
            PinataRewardEvent event = new PinataRewardEvent(player, reward, pool);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                rewards.award(player, reward, placeholders(player));
                statistics.addReward(player.getUniqueId());
            }
        });
    }

    public List<PinataContribution> leaderboard() {
        return hits.entrySet().stream().map(e -> new PinataContribution(e.getKey(), names.getOrDefault(e.getKey(), "Unknown"), e.getValue()))
                .sorted(Comparator.comparingInt(PinataContribution::hits).reversed().thenComparing(PinataContribution::name)).toList();
    }

    public Map<String, String> placeholders(Player player) {
        List<PinataContribution> board = leaderboard();
        Map<String, String> ph = new HashMap<>();
        ph.put("%player%", player == null ? "" : player.getName());
        ph.put("%hits%", String.valueOf(totalHits));
        ph.put("%remaining_hits%", String.valueOf(Math.max(0, type.requiredHits() - totalHits)));
        ph.put("%required_hits%", String.valueOf(type.requiredHits()));
        ph.put("%progress%", String.valueOf((int) ((totalHits * 100.0) / type.requiredHits())));
        ph.put("%top_player%", board.isEmpty() ? "None" : board.get(0).name());
        ph.put("%top_hits%", board.isEmpty() ? "0" : String.valueOf(board.get(0).hits()));
        ph.put("%time_remaining%", String.valueOf(Math.max(0, type.durationSeconds() - Duration.between(started, Instant.now()).getSeconds())));
        return ph;
    }

    private void particle(String name, int count) {
        try { entity.getWorld().spawnParticle(Particle.valueOf(name.toUpperCase()), entity.getLocation().add(0, 1, 0), count, .6, .6, .6, .02); } catch (IllegalArgumentException ignored) {}
    }

    private void play(Optional<org.bukkit.Sound> sound) {
        sound.ifPresent(s -> entity.getWorld().playSound(entity.getLocation(), s, 1f, 1f));
    }

    public PinataType type() { return type; }
    public Entity entity() { return entity; }
    public int totalHits() { return totalHits; }
    public boolean ended() { return ended; }
    public boolean allowsProjectileHits() { return type.projectileHits(); }
}
