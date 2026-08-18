package net.scape.project.supremepinata.config;

import net.kyori.adventure.text.Component;
import net.scape.project.supremepinata.utility.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private String prefix;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.prefix = config.getString("prefix", "<gradient:#ff4d6d:#ffd166><bold>SupremePinata</bold></gradient> <dark_gray>»</dark_gray>");
    }

    public Component component(String key, Map<String, String> placeholders) {
        Map<String, String> merged = new HashMap<>(placeholders);
        merged.put("%prefix%", prefix);
        return Text.parse(config.getString(key, "%prefix% <red>Missing message: " + key), merged);
    }

    public String raw(String key) {
        return config.getString(key, "");
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }
}
