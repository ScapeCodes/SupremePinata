package net.scape.project.supremepinata.integration;

import net.milkbowl.vault.economy.Economy;
import net.scape.project.supremepinata.statistics.StatisticsService;
import net.scape.project.supremepinata.trigger.VotePartyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class IntegrationManager {
    private final JavaPlugin plugin;
    private Economy economy;
    private PlaceholderHook placeholderHook;

    public IntegrationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable(StatisticsService statistics, VotePartyService votes) {
        reload();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderHook = new PlaceholderHook(plugin, statistics, votes);
            placeholderHook.register();
            plugin.getLogger().info("PlaceholderAPI hook enabled.");
        }
    }

    public void reload() {
        economy = null;
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
    }

    public void disable() {
        if (placeholderHook != null) placeholderHook.unregister();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        economy.depositPlayer(player, amount);
        return true;
    }

    public boolean vaultEnabled() {
        return economy != null;
    }

    public boolean placeholderApiEnabled() {
        return placeholderHook != null;
    }

    public boolean pluginEnabled(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}
