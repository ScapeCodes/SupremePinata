package net.scape.project.supremepinata.utility.menu.guis;

import net.scape.project.supremepinata.SupremePinata;
import net.scape.project.supremepinata.config.MessageService;
import net.scape.project.supremepinata.utility.menu.Menu;
import net.scape.project.supremepinata.utility.menu.MenuUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class PinataEditorMenu extends Menu {
    private final SupremePinata plugin;
    private final MessageService messages;
    private final File file;
    private final YamlConfiguration cfg;
    private final String id;

    public PinataEditorMenu(MenuUtil menuUtil, SupremePinata plugin, MessageService messages, String id) {
        super(menuUtil);
        this.plugin = plugin;
        this.messages = messages;
        this.id = id.toLowerCase();
        this.file = new File(plugin.getDataFolder(), "pinatas/" + this.id + ".yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    @Override public String getMenuName() { return "&d&l✦ Pinata Editor: &f" + id; }
    @Override public int getSlots() { return 54; }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        switch (slot) {
            case 10 -> adjustInt("event.required-hits", click.isRightClick() ? -10 : 10, 1);
            case 11 -> adjustInt("event.duration", click.isRightClick() ? -30 : 30, 5);
            case 12 -> adjustInt("event.hit-cooldown-ms", click.isRightClick() ? -50 : 50, 0);
            case 13 -> adjustDouble("event.max-distance", click.isRightClick() ? -1 : 1, 1);
            case 19 -> toggle("movement.enabled");
            case 20 -> adjustDouble("movement.speed", click.isRightClick() ? -0.05 : 0.05, 0.05);
            case 21 -> adjustDouble("movement.radius", click.isRightClick() ? -1 : 1, 1);
            case 28 -> toggle("entity.invulnerable");
            case 29 -> toggle("entity.glow");
            case 30 -> toggle("entity.rainbow");
            case 31 -> adjustInt("entity.rainbow-interval-ticks", click.isRightClick() ? -1 : 1, 1);
            case 37 -> toggle("bossbar.enabled");
            case 38 -> toggle("hologram.enabled");
            case 39 -> adjustDouble("hologram.height", click.isRightClick() ? -0.1 : 0.1, 0.5);
            case 40 -> toggle("event.projectile-hits");
            case 41 -> adjustInt("event.minimum-participation", click.isRightClick() ? -1 : 1, 0);
            case 42 -> toggle("event.survival-only-hits");
            case 45 -> new PinataSelectorMenu(menuUtil, plugin, messages, plugin.getPinataRegistry(), plugin.getPinataManager(), true).open();
            case 49 -> menuUtil.getOwner().closeInventory();
            default -> { return; }
        }
        saveReload();
        refresh();
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(10, stat(Material.TARGET, "Required Hits", "event.required-hits", "+/- 10"));
        inventory.setItem(11, stat(Material.CLOCK, "Duration", "event.duration", "+/- 30 seconds"));
        inventory.setItem(12, stat(Material.REPEATER, "Hit Cooldown", "event.hit-cooldown-ms", "+/- 50ms"));
        inventory.setItem(13, stat(Material.ENDER_PEARL, "Max Distance", "event.max-distance", "+/- 1 block"));
        inventory.setItem(19, bool(Material.FEATHER, "Movement", "movement.enabled"));
        inventory.setItem(20, stat(Material.SUGAR, "Movement Speed", "movement.speed", "+/- 0.05"));
        inventory.setItem(21, stat(Material.COMPASS, "Movement Radius", "movement.radius", "+/- 1 block"));
        inventory.setItem(28, bool(Material.SHIELD, "Invulnerable", "entity.invulnerable"));
        inventory.setItem(29, bool(Material.GLOW_INK_SAC, "Glow", "entity.glow"));
        inventory.setItem(30, bool(Material.AMETHYST_SHARD, "Rainbow", "entity.rainbow"));
        inventory.setItem(31, stat(Material.NOTE_BLOCK, "Rainbow Speed", "entity.rainbow-interval-ticks", "+/- 1 tick"));
        inventory.setItem(37, bool(Material.ENDER_EYE, "BossBar", "bossbar.enabled"));
        inventory.setItem(38, bool(Material.NAME_TAG, "Hologram", "hologram.enabled"));
        inventory.setItem(39, stat(Material.ARMOR_STAND, "Hologram Height", "hologram.height", "+/- 0.1 blocks"));
        inventory.setItem(40, bool(Material.ARROW, "Projectile Hits", "event.projectile-hits"));
        inventory.setItem(41, stat(Material.PLAYER_HEAD, "Minimum Participation", "event.minimum-participation", "+/- 1 player"));
        inventory.setItem(42, bool(Material.IRON_SWORD, "Survival Only Hits", "event.survival-only-hits"));
        inventory.setItem(45, item(Material.ARROW, "&e&lBack", "&7Return to pinata selection."));
        inventory.setItem(49, item(Material.BARRIER, "&c&lClose", "&7Autosaves and reloads on every click."));
        fill();
    }

    private org.bukkit.inventory.ItemStack stat(Material material, String name, String path, String step) {
        return item(material, "&d&l✦ " + name, List.of("&7Current: &f" + cfg.get(path), "&aLeft-click: increase " + step, "&cRight-click: decrease " + step, "&eAutosaves + reloads"));
    }

    private org.bukkit.inventory.ItemStack bool(Material material, String name, String path) {
        boolean value = cfg.getBoolean(path);
        return item(material, (value ? "&a&l✔ " : "&c&l✖ ") + "&d&l" + name, List.of("&7Current: " + (value ? "&aEnabled" : "&cDisabled"), "&eClick to toggle", "&eAutosaves + reloads"));
    }

    private void toggle(String path) { cfg.set(path, !cfg.getBoolean(path)); }
    private void adjustInt(String path, int delta, int min) { cfg.set(path, Math.max(min, cfg.getInt(path) + delta)); }
    private void adjustDouble(String path, double delta, double min) { cfg.set(path, Math.max(min, Math.round((cfg.getDouble(path) + delta) * 100.0) / 100.0)); }

    private void saveReload() {
        try {
            cfg.save(file);
            plugin.reloadServices(false);
            messages.send(menuUtil.getOwner(), "menu-editor-saved");
        } catch (IOException ex) {
            messages.send(menuUtil.getOwner(), "menu-editor-save-failed");
        }
    }
}
