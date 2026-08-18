package net.scape.project.supremepinata.command;

import net.scape.project.supremepinata.SupremePinata;
import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.config.PinataRegistry;
import net.scape.project.supremepinata.location.LocationService;
import net.scape.project.supremepinata.pinata.PinataManager;
import net.scape.project.supremepinata.statistics.DataSettings;
import net.scape.project.supremepinata.statistics.PlayerStats;
import net.scape.project.supremepinata.statistics.StatisticsService;
import net.scape.project.supremepinata.trigger.VotePartyService;
import net.scape.project.supremepinata.utility.menu.guis.PinataEditorMenu;
import net.scape.project.supremepinata.utility.menu.guis.PinataMainMenu;
import net.scape.project.supremepinata.utility.menu.guis.VoteSitesMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PinataCommand implements CommandExecutor, TabCompleter {
    private final SupremePinata plugin;
    private final MessageService messages;
    private final PinataRegistry registry;
    private final PinataManager manager;
    private final LocationService locations;
    private final VotePartyService votes;
    private final StatisticsService statistics;

    public PinataCommand(SupremePinata plugin, MessageService messages, PinataRegistry registry, PinataManager manager, LocationService locations, VotePartyService votes, StatisticsService statistics) {
        this.plugin = plugin;
        this.messages = messages;
        this.registry = registry;
        this.manager = manager;
        this.locations = locations;
        this.votes = votes;
        this.statistics = statistics;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) return help(sender);
        return switch (args[0].toLowerCase()) {
            case "spawn" -> spawn(sender, args);
            case "stop" -> stop(sender);
            case "info" -> info(sender);
            case "menu" -> menu(sender);
            case "editor", "edit" -> editor(sender, args);
            case "vote", "votesites" -> vote(sender);
            case "reload" -> reload(sender);
            case "location" -> location(sender, args);
            case "stats", "statistics" -> stats(sender, args);
            case "debug" -> debug(sender);
            default -> { messages.send(sender, "unknown-command"); yield true; }
        };
    }

    private boolean spawn(CommandSender sender, String[] args) {
        if (!perm(sender, "supremepinata.command.spawn")) return true;
        if (args.length < 2) { messages.send(sender, "usage-spawn"); return true; }
        Location loc = resolveSpawnLocation(sender, args).orElse(null);
        boolean spawned = manager.spawn(args[1], loc);
        messages.send(sender, spawned ? "spawn-success" : "spawn-failed", Map.of("%type%", args[1]));
        return true;
    }

    private java.util.Optional<Location> resolveSpawnLocation(CommandSender sender, String[] args) {
        if (args.length >= 6) return parseLocation(sender, args);
        return sender instanceof Player player ? java.util.Optional.of(player.getLocation()) : java.util.Optional.empty();
    }

    private java.util.Optional<Location> parseLocation(CommandSender sender, String[] args) {
        World world = Bukkit.getWorld(args[5]);
        if (world == null) {
            messages.send(sender, "invalid-world");
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(new Location(world, Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4])));
        } catch (NumberFormatException ex) {
            messages.send(sender, "invalid-location");
            return java.util.Optional.empty();
        }
    }

    private boolean stop(CommandSender sender) {
        if (!perm(sender, "supremepinata.command.stop")) return true;
        manager.stopActiveEvent("command");
        messages.send(sender, "stop-success");
        return true;
    }

    private boolean info(CommandSender sender) {
        if (!perm(sender, "supremepinata.command.info")) return true;
        manager.active().ifPresentOrElse(active -> messages.send(sender, "info-active", Map.of("%type%", active.type().id(), "%hits%", String.valueOf(active.totalHits()), "%required_hits%", String.valueOf(active.type().requiredHits()))), () -> messages.send(sender, "info-none"));
        return true;
    }

    private boolean menu(CommandSender sender) {
        if (!perm(sender, "supremepinata.command.menu")) return true;
        if (!(sender instanceof Player player)) { messages.send(sender, "player-only"); return true; }
        new PinataMainMenu(SupremePinata.getMenuUtil(player), plugin, messages, registry, manager, votes).open();
        return true;
    }

    private boolean editor(CommandSender sender, String[] args) {
        if (!perm(sender, "supremepinata.command.editor")) return true;
        if (!(sender instanceof Player player)) { messages.send(sender, "player-only"); return true; }
        if (args.length < 2) { messages.send(sender, "usage-editor"); return true; }
        if (registry.get(args[1]).isEmpty()) { messages.send(sender, "unknown-pinata", Map.of("%type%", args[1])); return true; }
        new PinataEditorMenu(SupremePinata.getMenuUtil(player), plugin, messages, args[1]).open();
        return true;
    }

    private boolean vote(CommandSender sender) {
        if (!perm(sender, "supremepinata.command.vote")) return true;
        if (!(sender instanceof Player player)) { messages.send(sender, "player-only"); return true; }
        new VoteSitesMenu(SupremePinata.getMenuUtil(player), plugin).open();
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!perm(sender, "supremepinata.command.reload")) return true;
        plugin.reloadServices(plugin.getConfig().getBoolean("settings.reload.stop-active-event", true));
        messages.send(sender, "reload-success");
        return true;
    }

    private boolean location(CommandSender sender, String[] args) {
        if (!perm(sender, "supremepinata.command.location")) return true;
        if (args.length < 2) { messages.send(sender, "usage-location"); return true; }
        try {
            handleLocationSubcommand(sender, args);
        } catch (IOException ex) { messages.send(sender, "location-save-failed"); }
        return true;
    }

    private void handleLocationSubcommand(CommandSender sender, String[] args) throws IOException {
        if (args[1].equalsIgnoreCase("set") && args.length >= 3 && sender instanceof Player player) {
            setLocation(sender, args, player);
            return;
        }
        if (args[1].equalsIgnoreCase("delete") && args.length >= 3) {
            locations.delete(args[2]);
            messages.send(sender, "location-delete", Map.of("%name%", args[2]));
            return;
        }
        if (args[1].equalsIgnoreCase("list")) {
            messages.send(sender, "location-list", Map.of("%locations%", String.join(", ", locations.names())));
            return;
        }
        messages.send(sender, "usage-location");
    }

    private void setLocation(CommandSender sender, String[] args, Player player) throws IOException {
        String target = args[2].toLowerCase();
        String locationName = args.length >= 4 ? args[3].toLowerCase() : target;
        locations.set(locationName, player.getLocation());
        if (registry.get(target).isPresent()) {
            addLocationToPinata(target, locationName);
            plugin.reloadServices(false);
            messages.send(sender, "location-set-pinata", Map.of("%type%", target, "%name%", locationName));
            return;
        }
        messages.send(sender, "location-set", Map.of("%name%", locationName));
    }

    private void addLocationToPinata(String type, String locationName) throws IOException {
        File file = new File(plugin.getDataFolder(), "pinatas/" + type.toLowerCase() + ".yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        Set<String> names = new LinkedHashSet<>(cfg.getStringList("locations"));
        names.add(locationName.toLowerCase());
        cfg.set("locations", new ArrayList<>(names));
        cfg.save(file);
    }

    private boolean stats(CommandSender sender, String[] args) {
        if (!perm(sender, "supremepinata.command.stats")) return true;
        OfflinePlayer target = resolveStatsTarget(sender, args);
        if (target == null || target.getUniqueId() == null) {
            messages.send(sender, "usage-stats");
            return true;
        }

        messages.send(sender, "stats-loading", Map.of("%player%", target.getName() == null ? target.getUniqueId().toString() : target.getName()));
        statistics.load(target.getUniqueId()).thenAccept(stats -> sendStats(sender, target, stats));
        return true;
    }

    private OfflinePlayer resolveStatsTarget(CommandSender sender, String[] args) {
        if (args.length >= 2) return Bukkit.getOfflinePlayer(args[1]);
        return sender instanceof Player player ? player : null;
    }

    private void sendStats(CommandSender sender, OfflinePlayer target, PlayerStats stats) {
        String name = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        messages.send(sender, "stats", Map.of(
                "%player%", name,
                "%total_hits%", String.valueOf(stats.totalHits()),
                "%parties_participated%", String.valueOf(stats.partiesParticipated()),
                "%parties_won%", String.valueOf(stats.partiesWon()),
                "%final_hits%", String.valueOf(stats.finalHits()),
                "%rewards_won%", String.valueOf(stats.rewardsWon())
        ));
    }

    private boolean debug(CommandSender sender) {
        if (!perm(sender, "supremepinata.command.debug")) return true;

        DataSettings data = plugin.getDataFile().settings();
        messages.send(sender, "debug", debugPlaceholders(data));
        return true;
    }

    private Map<String, String> debugPlaceholders(DataSettings data) {
        Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("%version%", plugin.getDescription().getVersion());
        placeholders.put("%author%", String.join(", ", plugin.getDescription().getAuthors()));
        placeholders.put("%discord%", "N/A");
        placeholders.put("%pinatas_loaded%", String.valueOf(registry.types().size()));
        placeholders.put("%locations_loaded%", String.valueOf(locations.names().size()));
        placeholders.put("%db_type%", data.type().name());
        placeholders.put("%db_connected%", "true");
        placeholders.put("%hook_vault%", enabled(plugin.getIntegrationManager().vaultEnabled()));
        placeholders.put("%hook_placeholderapi%", enabled(plugin.getIntegrationManager().placeholderApiEnabled()));
        placeholders.put("%hook_nuvotifier%", enabled(plugin.getIntegrationManager().pluginEnabled("NuVotifier")));
        placeholders.put("%hook_votifierplus%", enabled(plugin.getIntegrationManager().pluginEnabled("VotifierPlus")));
        placeholders.put("%config_errors%", configErrors());
        placeholders.put("%papi_test_votes%", "%supremepinata_votes%");
        placeholders.put("%papi_test_wins%", "%supremepinata_wins%");
        return placeholders;
    }

    private String enabled(boolean value) {
        return value ? "&aEnabled" : "&cDisabled";
    }

    private String configErrors() {
        List<String> errors = new ArrayList<>();
        if (registry.types().isEmpty()) errors.add("&8- &cNo pinata types loaded.");
        if (locations.names().isEmpty()) errors.add("&8- &eNo saved party locations loaded.");
        if (errors.isEmpty()) return "&8- &aNo configuration problems found.";
        return String.join("\n", errors);
    }

    private boolean help(CommandSender sender) {
        if (!perm(sender, "supremepinata.command.help")) return true;
        messages.send(sender, "help");
        return true;
    }

    private boolean perm(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("supremepinata.admin")) return true;
        messages.send(sender, "no-permission");
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return filter(List.of("spawn", "stop", "info", "menu", "editor", "vote", "reload", "help", "location", "stats", "debug"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return filter(new ArrayList<>(registry.types().keySet()), args[1]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("editor") || args[0].equalsIgnoreCase("edit"))) return filter(new ArrayList<>(registry.types().keySet()), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("location")) return filter(List.of("set", "delete", "list"), args[1]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("statistics"))) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("location") && args[1].equalsIgnoreCase("set")) return filter(new ArrayList<>(registry.types().keySet()), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("location") && args[1].equalsIgnoreCase("set")) return filter(List.of(args[2].toLowerCase()), args[3]);
        if (args.length == 3 && args[0].equalsIgnoreCase("location") && args[1].equalsIgnoreCase("delete")) return filter(locations.names(), args[2]);
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        return values.stream().filter(v -> v.toLowerCase().startsWith(input.toLowerCase())).toList();
    }
}
