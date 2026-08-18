package net.scape.project.supremepinata.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.scape.project.supremepinata.statistics.PlayerStats;
import net.scape.project.supremepinata.statistics.StatisticsService;
import net.scape.project.supremepinata.trigger.VotePartyService;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class PlaceholderHook extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final StatisticsService statistics;
    private final VotePartyService votes;

    public PlaceholderHook(JavaPlugin plugin, StatisticsService statistics, VotePartyService votes) {
        this.plugin = plugin;
        this.statistics = statistics;
        this.votes = votes;
    }

    @Override public @NotNull String getIdentifier() { return "supremepinata"; }
    @Override public @NotNull String getAuthor() { return String.join(",", plugin.getDescription().getAuthors()); }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("votes")) return String.valueOf(votes.currentVotes());
        if (params.equalsIgnoreCase("votes_required")) return String.valueOf(votes.requiredVotes());
        if (params.equalsIgnoreCase("votes_remaining")) return String.valueOf(Math.max(0, votes.requiredVotes() - votes.currentVotes()));
        if (player == null || player.getUniqueId() == null) return "0";
        PlayerStats stats = statistics.load(player.getUniqueId()).getNow(PlayerStats.empty(player.getUniqueId()));
        return switch (params.toLowerCase()) {
            case "hits" -> String.valueOf(stats.totalHits());
            case "parties" -> String.valueOf(stats.partiesParticipated());
            case "wins" -> String.valueOf(stats.partiesWon());
            case "final_hits" -> String.valueOf(stats.finalHits());
            case "rewards" -> String.valueOf(stats.rewardsWon());
            default -> null;
        };
    }
}
