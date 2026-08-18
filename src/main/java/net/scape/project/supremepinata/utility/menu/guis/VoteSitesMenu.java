package net.scape.project.supremepinata.utility.menu.guis;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.scape.project.supremepinata.SupremePinata;
import net.scape.project.supremepinata.utility.Text;
import net.scape.project.supremepinata.utility.menu.Menu;
import net.scape.project.supremepinata.utility.menu.MenuUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VoteSitesMenu extends Menu {
    private final SupremePinata plugin;
    private final List<VoteSite> sites = new ArrayList<>();

    public VoteSitesMenu(MenuUtil menuUtil, SupremePinata plugin) {
        super(menuUtil);
        this.plugin = plugin;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("vote-sites");
        if (section != null) {
            for (String key : section.getKeys(false)) sites.add(new VoteSite(section.getString(key + ".name", key), section.getString(key + ".url", "")));
        }
    }

    @Override public String getMenuName() { return "&a&l✔ Vote Sites ✦"; }
    @Override public int getSlots() { return 27; }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot >= sites.size()) return;
        VoteSite site = sites.get(slot);
        menuUtil.getOwner().closeInventory();
        menuUtil.getOwner().sendMessage(Text.parse("<dark_gray>[</dark_gray><gradient:#55ff55:#55ffff><bold>VOTE</bold></gradient><dark_gray>]</dark_gray> <green>✔</green> <yellow>Click to open:</yellow> <aqua><underlined>" + site.name() + "</underlined></aqua>", Map.of())
                .clickEvent(ClickEvent.openUrl(site.url()))
                .hoverEvent(HoverEvent.showText(Component.text(site.url()))));
    }

    @Override
    public void setMenuItems() {
        for (int i = 0; i < sites.size() && i < 27; i++) {
            VoteSite site = sites.get(i);
            inventory.setItem(i, item(Material.EMERALD, "&a&l✔ " + site.name(), "&7Click to receive the vote link.", "&b" + site.url()));
        }
        if (sites.isEmpty()) inventory.setItem(13, item(Material.BARRIER, "&c&lNo vote sites configured", "&7Add links under &fvote-sites &7in config.yml."));
        fill();
    }

    private record VoteSite(String name, String url) {}
}
