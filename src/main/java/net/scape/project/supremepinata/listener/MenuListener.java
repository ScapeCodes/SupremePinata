package net.scape.project.supremepinata.listener;

import net.scape.project.supremepinata.SupremePinata;
import net.scape.project.supremepinata.utility.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory topInventory = e.getView().getTopInventory();
        if (topInventory == null) return;

        if (topInventory.getHolder() instanceof Menu menu) {
            e.setCancelled(true);

            if (e.getClickedInventory() != null && e.getClickedInventory().equals(topInventory)) {
                ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    menu.handleMenu(e);
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory topInventory = e.getView().getTopInventory();
        if (topInventory == null) return;

        if (topInventory.getHolder() instanceof Menu) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if (e.getInventory().getHolder() instanceof Menu) {
            SupremePinata.getInstance().getMenuUtil().remove(p.getUniqueId());
        }
    }
}
