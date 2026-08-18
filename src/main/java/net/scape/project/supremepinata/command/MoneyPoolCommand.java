package net.scape.project.supremepinata.command;

import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.integration.IntegrationManager;
import net.scape.project.supremepinata.trigger.MoneyPoolService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MoneyPoolCommand implements CommandExecutor, TabCompleter {
    private final MessageService messages;
    private final MoneyPoolService moneyPool;
    private final IntegrationManager integrations;

    public MoneyPoolCommand(MessageService messages, MoneyPoolService moneyPool, IntegrationManager integrations) {
        this.messages = messages;
        this.moneyPool = moneyPool;
        this.integrations = integrations;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            messages.send(sender, "money-pool-status", moneyPool.placeholders());
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (!sender.hasPermission("supremepinata.command.pool")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (!moneyPool.enabled()) {
            messages.send(sender, "money-pool-disabled");
            return true;
        }
        if (!integrations.vaultEnabled()) {
            messages.send(sender, "vault-required");
            return true;
        }

        double amount = parseAmount(args[0]);
        if (amount <= 0.0D) {
            messages.send(sender, "money-pool-invalid-amount");
            return true;
        }
        if (!moneyPool.add(player, amount)) {
            messages.send(sender, "money-pool-payment-failed", moneyPool.placeholders());
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? List.of("100", "500", "1000", "5000") : List.of();
    }

    private double parseAmount(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ex) {
            return -1.0D;
        }
    }
}
