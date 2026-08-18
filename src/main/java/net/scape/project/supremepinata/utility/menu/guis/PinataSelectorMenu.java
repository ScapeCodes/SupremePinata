package net.scape.project.supremepinata.utility.menu.guis;

import net.scape.project.supremepinata.SupremePinata;
import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.config.PinataRegistry;
import net.scape.project.supremepinata.pinata.PinataManager;
import net.scape.project.supremepinata.pinata.PinataType;
import net.scape.project.supremepinata.utility.menu.Menu;
import net.scape.project.supremepinata.utility.menu.MenuUtil;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PinataSelectorMenu extends Menu {
    private final SupremePinata plugin;
    private final MessageService messages;
    private final PinataRegistry registry;
    private final PinataManager manager;
    private final boolean editMode;
    private List<PinataType> types;

    public PinataSelectorMenu(MenuUtil menuUtil, SupremePinata plugin, MessageService messages, PinataRegistry registry, PinataManager manager, boolean editMode) {
        super(menuUtil);
        this.plugin = plugin;
        this.messages = messages;
        this.registry = registry;
        this.manager = manager;
        this.editMode = editMode;
        this.types = sortedTypes();
    }

    @Override public String getMenuName() { return editMode ? "&d&l✦ Select Pinata To Edit" : "&a&l✔ Select Pinata To Spawn"; }
    @Override public int getSlots() { return 54; }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 49) {
            new PinataMainMenu(menuUtil, plugin, messages, registry, manager, plugin.getVotePartyService()).open();
            return;
        }
        if (slot < 0 || slot >= types.size()) return;
        PinataType type = types.get(slot);
        if (editMode) {
            new PinataEditorMenu(menuUtil, plugin, messages, type.id()).open();
            return;
        }
        boolean spawned = manager.spawn(type.id(), menuUtil.getOwner().getLocation());
        menuUtil.getOwner().closeInventory();
        messages.send(menuUtil.getOwner(), spawned ? "spawn-success" : "spawn-failed", Map.of("%type%", type.id()));
    }

    @Override
    public void setMenuItems() {
        this.types = sortedTypes();
        for (int i = 0; i < types.size() && i < 45; i++) {
            PinataType type = types.get(i);
            inventory.setItem(i, item(Material.LLAMA_SPAWN_EGG, "&d&l✦ " + type.id(), List.of(
                    "&7Display: &f" + type.displayName(),
                    "&7Hits: &f" + type.requiredHits(),
                    "&7Duration: &f" + type.durationSeconds() + "s",
                    editMode ? "&eClick to edit this pinata." : "&eClick to spawn at your location."
            )));
        }
        if (types.isEmpty()) inventory.setItem(22, item(Material.BARRIER, "&c&lNo pinatas loaded", "&7Add yml files to the pinatas folder."));
        inventory.setItem(49, item(Material.ARROW, "&e&lBack", "&7Return to the dashboard."));
        fill();
    }

    private List<PinataType> sortedTypes() {
        return registry.types().values().stream().sorted(Comparator.comparing(PinataType::id)).toList();
    }
}
