package net.scape.project.supremepinata.utility.menu;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public abstract class Menu implements InventoryHolder {
    protected final MenuUtil menuUtil;
    protected Inventory inventory;

    protected Menu(MenuUtil menuUtil) {
        this.menuUtil = menuUtil;
    }

    public abstract String getMenuName();
    public abstract int getSlots();
    public abstract void handleMenu(InventoryClickEvent event);
    public abstract void setMenuItems();

    public void open() {
        inventory = Bukkit.createInventory(this, getSlots(), color(getMenuName()));
        setMenuItems();
        menuUtil.getOwner().openInventory(inventory);
    }

    public void refresh() {
        if (inventory == null) return;
        inventory.clear();
        setMenuItems();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected ItemStack item(Material material, String displayName, String... lore) {
        return item(material, displayName, Arrays.asList(lore));
    }

    protected ItemStack item(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName));
            meta.lore(lore.stream().map(line -> LegacyComponentSerializer.legacyAmpersand().deserialize(line)).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    protected void fill() {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) if (inventory.getItem(i) == null) inventory.setItem(i, filler);
    }

    protected String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }
}
