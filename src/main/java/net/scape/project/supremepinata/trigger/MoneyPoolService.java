package net.scape.project.supremepinata.trigger;

import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.integration.IntegrationManager;
import net.scape.project.supremepinata.pinata.PinataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class MoneyPoolService {
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final PinataManager manager;
    private final IntegrationManager integrations;
    private boolean enabled;
    private double target;
    private double current;
    private String pinata;
    private boolean resetAfterSpawn;

    public MoneyPoolService(JavaPlugin plugin, MessageService messages, PinataManager manager, IntegrationManager integrations) {
        this.plugin = plugin;
        this.messages = messages;
        this.manager = manager;
        this.integrations = integrations;
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        enabled = cfg.getBoolean("money-pool.enabled", true);
        target = Math.max(0.01D, cfg.getDouble("money-pool.target", 50000.0D));
        current = Math.max(0.0D, cfg.getDouble("money-pool.current", 0.0D));
        pinata = cfg.getString("money-pool.pinata", "money");
        resetAfterSpawn = cfg.getBoolean("money-pool.reset-after-spawn", true);
    }

    public boolean add(Player player, double amount) {
        if (!enabled || amount <= 0.0D || !integrations.withdraw(player, amount)) return false;
        current += amount;
        saveCurrent();
        Bukkit.broadcast(messages.component("money-pool-contribution", placeholders(player.getName(), amount)));
        checkSpawn();
        return true;
    }

    public Map<String, String> placeholders() {
        return placeholders("", 0.0D);
    }

    private Map<String, String> placeholders(String player, double amount) {
        return Map.of(
                "%player%", player,
                "%amount%", integrations.format(amount),
                "%current%", integrations.format(current),
                "%target%", integrations.format(target),
                "%remaining%", integrations.format(Math.max(0.0D, target - current)),
                "%pinata%", pinata
        );
    }

    private void checkSpawn() {
        if (current < target) return;
        Bukkit.broadcast(messages.component("money-pool-complete", placeholders()));
        boolean spawned = manager.spawn(pinata, null);
        if (spawned && resetAfterSpawn) {
            current = 0.0D;
            saveCurrent();
        }
    }

    private void saveCurrent() {
        plugin.getConfig().set("money-pool.current", current);
        plugin.saveConfig();
    }

    public boolean enabled() { return enabled; }
    public double current() { return current; }
    public double target() { return target; }
    public String pinata() { return pinata; }
}
