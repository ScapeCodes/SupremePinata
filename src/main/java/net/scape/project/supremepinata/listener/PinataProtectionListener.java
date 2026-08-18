package net.scape.project.supremepinata.listener;

import net.scape.project.supremepinata.pinata.PinataManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class PinataProtectionListener implements Listener {
    private final PinataManager manager;

    public PinataProtectionListener(PinataManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!isPinata(event.getEntity())) return;
        event.setCancelled(true);
        event.setDamage(0.0D);
        if (event.getDamager() instanceof Projectile && manager.active().map(active -> !active.allowsProjectileHits()).orElse(true)) return;
        Player player = attacker(event.getDamager());
        if (player != null) manager.handleHit(player, event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (isPinata(event.getEntity())) {
            event.setCancelled(true);
            event.setDamage(0.0D);
        }
    }

    @EventHandler public void onDeath(EntityDeathEvent event) { if (isPinata(event.getEntity())) { event.getDrops().clear(); event.setDroppedExp(0); } }
    @EventHandler public void onDrop(EntityDropItemEvent event) { if (isPinata(event.getEntity())) event.setCancelled(true); }
    @EventHandler public void onBreed(EntityBreedEvent event) { if (isPinata(event.getEntity())) event.setCancelled(true); }
    @EventHandler public void onMount(EntityMountEvent event) { if (isPinata(event.getMount())) event.setCancelled(true); }
    @EventHandler public void onBucket(PlayerBucketEntityEvent event) { if (isPinata(event.getEntity())) event.setCancelled(true); }
    @EventHandler public void onLeash(PlayerLeashEntityEvent event) { if (isPinata(event.getEntity())) event.setCancelled(true); }
    @EventHandler public void onInteract(PlayerInteractEntityEvent event) { if (isPinata(event.getRightClicked())) event.setCancelled(true); }

    private boolean isPinata(Entity entity) {
        return manager.active().map(active -> active.entity().getUniqueId().equals(entity.getUniqueId())).orElse(false);
    }

    private Player attacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }
}
