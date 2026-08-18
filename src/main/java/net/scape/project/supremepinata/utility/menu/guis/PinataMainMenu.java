package net.scape.project.supremepinata.utility.menu.guis;

import net.scape.project.supremepinata.SupremePinata;
import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.config.PinataRegistry;
import net.scape.project.supremepinata.pinata.PinataManager;
import net.scape.project.supremepinata.trigger.VotePartyService;
import net.scape.project.supremepinata.utility.menu.Menu;
import net.scape.project.supremepinata.utility.menu.MenuUtil;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public final class PinataMainMenu extends Menu {
    private final SupremePinata plugin;
    private final MessageService messages;
    private final PinataRegistry registry;
    private final PinataManager manager;
    private final VotePartyService votes;

    public PinataMainMenu(MenuUtil menuUtil, SupremePinata plugin, MessageService messages, PinataRegistry registry, PinataManager manager, VotePartyService votes) {
        super(menuUtil);
        this.plugin = plugin;
        this.messages = messages;
        this.registry = registry;
        this.manager = manager;
        this.votes = votes;
    }

    @Override public String getMenuName() { return "&d&l✦ SupremePinata"; }
    @Override public int getSlots() { return 27; }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 10 -> new PinataSelectorMenu(menuUtil, plugin, messages, registry, manager, false).open();
            case 12 -> new PinataSelectorMenu(menuUtil, plugin, messages, registry, manager, true).open();
            case 14 -> new VoteSitesMenu(menuUtil, plugin).open();
            case 16 -> {
                plugin.reloadServices(plugin.getConfig().getBoolean("settings.reload.stop-active-event", true));
                messages.send(menuUtil.getOwner(), "reload-success");
                refresh();
            }
            case 22 -> menuUtil.getOwner().closeInventory();
            default -> { }
        }
    }

    @Override
    public void setMenuItems() {
        String activeLine = manager.active()
                .map(active -> "&aActive: &f" + active.type().id() + " &7(" + active.totalHits() + "/" + active.type().requiredHits() + ")")
                .orElse("&7No active pinata event.");

        inventory.setItem(4, item(Material.NETHER_STAR, "&d&l✦ SupremePinata Dashboard", List.of(
                activeLine,
                "&7Loaded pinatas: &f" + registry.types().size(),
                "&7Vote party: &f" + votes.currentVotes() + "/" + votes.requiredVotes()
        )));
        inventory.setItem(10, item(Material.FIREWORK_ROCKET, "&a&lSpawn Pinata", "&7Choose a pinata type to spawn.", "&eClick to open."));
        inventory.setItem(12, item(Material.WRITABLE_BOOK, "&d&lEdit Pinata", "&7Choose a pinata config to edit.", "&eClick to open."));
        inventory.setItem(14, item(Material.EMERALD, "&a&lVote Sites", "&7View configured vote links.", "&eClick to open."));
        inventory.setItem(16, item(Material.REDSTONE, "&c&lReload Plugin", "&7Reload config, messages and pinatas.", "&eClick to reload."));
        inventory.setItem(22, item(Material.BARRIER, "&c&lClose"));
        fill();
    }
}
