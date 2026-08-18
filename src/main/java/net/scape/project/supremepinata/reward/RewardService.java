package net.scape.project.supremepinata.reward;

import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.integration.IntegrationManager;
import net.scape.project.supremepinata.utility.SchedulerService;
import net.scape.project.supremepinata.utility.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RewardService {
    private final JavaPlugin plugin;
    private final IntegrationManager integrations;
    private final MessageService messages;
    private SchedulerService scheduler;

    public RewardService(JavaPlugin plugin, IntegrationManager integrations, MessageService messages) {
        this.plugin = plugin;
        this.integrations = integrations;
        this.messages = messages;
    }

    public void scheduler(SchedulerService scheduler) {
        this.scheduler = scheduler;
    }

    public void reload() {}

    public RewardPool loadPool(ConfigurationSection section) {
        RewardPool pool = new RewardPool();
        if (section == null) return pool;
        for (String id : section.getKeys(false)) {
            ConfigurationSection rewardSection = section.getConfigurationSection(id);
            if (rewardSection == null) continue;
            pool.add(loadReward(id, rewardSection));
        }
        return pool;
    }

    private Reward loadReward(String id, ConfigurationSection section) {
        return new Reward(id, Math.max(1, section.getInt("weight", 1)), section.getStringList("commands"), section.getStringList("player-commands"), loadItems(section), section.getDouble("money", 0), section.getInt("experience", 0), Optional.ofNullable(section.getString("message")), Optional.ofNullable(section.getString("broadcast")), sound(section));
    }

    private List<ItemStack> loadItems(ConfigurationSection section) {
        List<ItemStack> items = new ArrayList<>();
        ConfigurationSection itemSection = section.getConfigurationSection("items");
        if (itemSection == null) return items;
        for (String key : itemSection.getKeys(false)) {
            ConfigurationSection item = itemSection.getConfigurationSection(key);
            if (item == null) continue;
            Material material = Material.matchMaterial(item.getString("material", "STONE"));
            if (material != null) items.add(new ItemStack(material, Math.max(1, item.getInt("amount", 1))));
        }
        return items;
    }

    private Optional<Sound> sound(ConfigurationSection section) {
        Optional<Sound> sound = Optional.empty();
        if (section.isString("sound")) {
            try { sound = Optional.of(Sound.valueOf(section.getString("sound", "").toUpperCase())); } catch (IllegalArgumentException ignored) {}
        }
        return sound;
    }

    public void award(Player player, Reward reward, Map<String, String> placeholders) {
        Map<String, String> ph = new java.util.HashMap<>(placeholders);
        ph.put("%player%", player.getName());
        ph.put("%reward%", reward.id());
        giveItems(player, reward.items());
        if (reward.experience() > 0) player.giveExp(reward.experience());
        if (reward.money() > 0) integrations.deposit(player, reward.money());
        runTask(() -> runCommands(player, reward, ph));
        reward.message().ifPresent(message -> player.sendMessage(Text.parse(message, ph)));
        reward.broadcast().ifPresent(message -> runTask(() -> Bukkit.broadcast(Text.parse(message, ph))));
        reward.sound().ifPresent(sound -> player.playSound(player.getLocation(), sound, 1f, 1f));
    }

    private void giveItems(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            player.getInventory().addItem(item.clone()).values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
        }
    }

    private void runCommands(Player player, Reward reward, Map<String, String> placeholders) {
        for (String command : reward.consoleCommands()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replace(command, placeholders));
        for (String command : reward.playerCommands()) player.performCommand(replace(command, placeholders));
    }

    private void runTask(Runnable task) {
        if (scheduler != null) scheduler.runGlobal(task); else task.run();
    }

    private String replace(String input, Map<String, String> placeholders) {
        String out = input;
        for (Map.Entry<String, String> e : placeholders.entrySet()) out = out.replace(e.getKey(), e.getValue());
        return out;
    }
}
