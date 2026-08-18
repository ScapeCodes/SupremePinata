package net.scape.project.supremepinata.config;

import net.scape.project.supremepinata.pinata.PinataType;
import net.scape.project.supremepinata.reward.RewardPool;
import net.scape.project.supremepinata.reward.RewardService;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PinataRegistry {
    private final JavaPlugin plugin;
    private final RewardService rewards;
    private final Map<String, PinataType> types = new HashMap<>();

    public PinataRegistry(JavaPlugin plugin, RewardService rewards) {
        this.plugin = plugin;
        this.rewards = rewards;
    }

    public void reload() {
        types.clear();
        File folder = new File(plugin.getDataFolder(), "pinatas");
        if (!folder.exists() && !folder.mkdirs()) plugin.getLogger().warning("Could not create pinatas directory.");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) load(file).ifPresent(type -> types.put(type.id().toLowerCase(), type));
    }

    public Optional<PinataType> get(String id) {
        return Optional.ofNullable(types.get(id.toLowerCase()));
    }

    public Map<String, PinataType> types() {
        return Map.copyOf(types);
    }

    public void register(PinataType type) {
        types.put(type.id().toLowerCase(), type);
    }

    private Optional<PinataType> load(File file) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String id = cfg.getString("id", file.getName().replace(".yml", "")).toLowerCase();
            EntityType entityType = EntityType.valueOf(cfg.getString("entity.type", "LLAMA").toUpperCase());
            PinataType type = new PinataType(
                    id,
                    cfg.getString("display-name", "<gold><bold>PINATA</bold>"),
                    entityType,
                    Math.max(1, cfg.getInt("event.required-hits", 100)),
                    Math.max(5, cfg.getInt("event.duration", 300)),
                    cfg.getDouble("movement.speed", 1.05),
                    cfg.getDouble("movement.radius", 16),
                    cfg.getBoolean("movement.enabled", true),
                    cfg.getBoolean("entity.knockback", false),
                    cfg.getBoolean("entity.invulnerable", true),
                    Math.max(0, cfg.getInt("event.hit-cooldown-ms", 450)),
                    cfg.getInt("event.max-hits-per-player", -1),
                    Math.max(0, cfg.getInt("event.minimum-participation", 1)),
                    cfg.getDouble("event.max-distance", 8),
                    cfg.getBoolean("event.projectile-hits", false),
                    cfg.getBoolean("event.survival-only-hits", true),
                    loadCosmetics(cfg),
                    cfg.getStringList("locations"),
                    loadBossBar(cfg),
                    loadHologram(cfg),
                    loadEffects(cfg),
                    loadPools(cfg)
            );
            return Optional.of(type);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to load pinata file " + file.getName() + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, RewardPool> loadPools(YamlConfiguration cfg) {
        Map<String, RewardPool> pools = new HashMap<>();
        ConfigurationSection poolRoot = cfg.getConfigurationSection("reward-pools");
        if (poolRoot == null) return pools;
        for (String pool : poolRoot.getKeys(false)) {
            pools.put(pool, rewards.loadPool(poolRoot.getConfigurationSection(pool)));
        }
        return pools;
    }

    private PinataType.Cosmetics loadCosmetics(YamlConfiguration cfg) {
        return new PinataType.Cosmetics(
                cfg.getBoolean("entity.glow", false),
                cfg.getBoolean("entity.rainbow", false),
                Math.max(1, cfg.getInt("entity.rainbow-interval-ticks", 10))
        );
    }

    private PinataType.BossBarSettings loadBossBar(YamlConfiguration cfg) {
        return new PinataType.BossBarSettings(
                cfg.getBoolean("bossbar.enabled", true),
                cfg.getString("bossbar.title", "<yellow>🪅 Pinata <gray>• <white>%hits%/%required_hits%"),
                cfg.getBoolean("bossbar.progress", true),
                cfg.getString("bossbar.color", "YELLOW"),
                cfg.getString("bossbar.overlay", "PROGRESS")
        );
    }

    private PinataType.HologramSettings loadHologram(YamlConfiguration cfg) {
        return new PinataType.HologramSettings(
                cfg.getBoolean("hologram.enabled", true),
                cfg.getDouble("hologram.height", 2.4),
                cfg.getStringList("hologram.lines")
        );
    }

    private PinataType.Effects loadEffects(YamlConfiguration cfg) {
        return new PinataType.Effects(
                sound(cfg.getString("sounds.spawn")),
                sound(cfg.getString("sounds.hit")),
                sound(cfg.getString("sounds.complete")),
                cfg.getString("particles.spawn", "HAPPY_VILLAGER"),
                cfg.getString("particles.hit", "CRIT"),
                cfg.getString("particles.complete", "FIREWORK")
        );
    }

    private Optional<Sound> sound(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        try { return Optional.of(Sound.valueOf(input.toUpperCase())); } catch (IllegalArgumentException ex) { return Optional.empty(); }
    }
}
