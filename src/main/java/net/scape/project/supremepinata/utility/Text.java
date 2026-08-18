package net.scape.project.supremepinata.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;

public final class Text {
    public static final MiniMessage MINI = MiniMessage.miniMessage();

    private Text() {}

    public static Component parse(String input, Map<String, String> placeholders) {
        String value = input == null ? "" : input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return MINI.deserialize(value);
    }
}
