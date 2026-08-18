package net.scape.project.supremepinata.trigger;

import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.pinata.PinataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class VotePartyService implements PinataTrigger {
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final PinataManager manager;
    private boolean enabled;
    private int requiredVotes;
    private int currentVotes;
    private String pinata;
    private boolean reset;

    public VotePartyService(JavaPlugin plugin, MessageService messages, PinataManager manager) {
        this.plugin = plugin;
        this.messages = messages;
        this.manager = manager;
    }

    @Override public String id() { return "vote"; }

    @Override
    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        enabled = cfg.getBoolean("settings.vote-party.enabled", true);
        requiredVotes = Math.max(1, cfg.getInt("settings.vote-party.required-votes", 100));
        currentVotes = cfg.getInt("settings.vote-party.current-votes", 0);
        pinata = cfg.getString("settings.vote-party.pinata", "vote");
        reset = cfg.getBoolean("settings.vote-party.reset-after-party", true);
    }

    public void addVote(String voter) {
        if (!enabled) return;
        currentVotes++;
        for (int milestone : plugin.getConfig().getIntegerList("settings.vote-party.announcements.milestones")) {
            if (currentVotes == milestone) Bukkit.broadcast(messages.component("vote-milestone", Map.of("%votes%", String.valueOf(currentVotes), "%required%", String.valueOf(requiredVotes))));
        }
        if (currentVotes >= requiredVotes) {
            Bukkit.broadcast(messages.component("vote-party-start", Map.of("%votes%", String.valueOf(currentVotes), "%required%", String.valueOf(requiredVotes))));
            manager.spawn(pinata, null);
            if (reset) currentVotes = 0;
            plugin.getConfig().set("settings.vote-party.current-votes", currentVotes);
            plugin.saveConfig();
        }
    }

    public int currentVotes() { return currentVotes; }
    public int requiredVotes() { return requiredVotes; }
    @Override public void shutdown() {}
}
