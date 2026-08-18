package net.scape.project.supremepinata.utility;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;

public class Utils {

    public static boolean hasAmount(Player player, String economyType, double cost, String tag) {
        if (economyType.equalsIgnoreCase("VAULT")) {
            Economy economy = vaultEconomy();
            return economy != null && economy.has(player, cost);
        } else if (economyType.equalsIgnoreCase("PLAYERPOINTS")) {
            Number balance = invokePlayerPoints(player.getUniqueId(), "look");
            return balance != null && balance.doubleValue() >= cost;
        } else if (economyType.equalsIgnoreCase("EXP_LEVEL")) {
            return player.getLevel() >= cost;
        } else if (economyType.startsWith("EXCELLENTECONOMY-")) {
            return false;
        }

        return false;
    }

    public static void take(Player player, String economyType, double cost, String tag) {
        if (economyType.equalsIgnoreCase("VAULT")) {
            Economy economy = vaultEconomy();
            if (economy != null) economy.withdrawPlayer(player, cost);
        } else if (economyType.equalsIgnoreCase("PLAYERPOINTS")) {
            invokePlayerPoints(player.getUniqueId(), "take", (int) cost);
        } else if (economyType.equalsIgnoreCase("EXP_LEVEL")) {
            player.setLevel((int) (player.getLevel() - cost));
        } else if (economyType.startsWith("EXCELLENTECONOMY-")) {
            // ExcellentEconomy support can be added here once its configured currency API is available.
        }
    }

    private static Economy vaultEconomy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return null;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    private static Number invokePlayerPoints(UUID uuid, String methodName, Object... args) {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
            if (plugin == null || !plugin.isEnabled()) return null;
            Object api = plugin.getClass().getMethod("getAPI").invoke(plugin);
            Class<?>[] parameterTypes = new Class<?>[args.length + 1];
            Object[] parameters = new Object[args.length + 1];
            parameterTypes[0] = UUID.class;
            parameters[0] = uuid;
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i + 1] = args[i] instanceof Integer ? int.class : args[i].getClass();
                parameters[i + 1] = args[i];
            }
            Method method = api.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(api, parameters);
            return result instanceof Number number ? number : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    /**
     * Supports comparisons like:
     * - >=, <=, >, <, ==, !=
     */
    private static boolean evaluateCondition(String condition) {
        condition = condition.replace(" ", "");

        String[] operators = {">=", "<=", "==", "!=", ">", "<"};

        for (String op : operators) {
            if (condition.contains(op)) {
                String[] parts = condition.split(java.util.regex.Pattern.quote(op));
                if (parts.length != 2) return false;

                double left, right;
                try {
                    left = Double.parseDouble(parts[0]);
                    right = Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    return false;
                }

                return switch (op) {
                    case ">=" -> left >= right;
                    case "<=" -> left <= right;
                    case ">" -> left > right;
                    case "<" -> left < right;
                    case "==" -> left == right;
                    case "!=" -> left != right;
                    default -> false;
                };
            }
        }

        return false;
    }
}
