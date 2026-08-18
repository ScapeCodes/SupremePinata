package net.scape.project.supremepinata.reward;

import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public record Reward(
        String id,
        int weight,
        List<String> consoleCommands,
        List<String> playerCommands,
        List<ItemStack> items,
        double money,
        int experience,
        Optional<String> message,
        Optional<String> broadcast,
        Optional<Sound> sound
) {}
