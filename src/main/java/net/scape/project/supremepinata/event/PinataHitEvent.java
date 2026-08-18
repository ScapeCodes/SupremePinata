package net.scape.project.supremepinata.event;

import net.scape.project.supremepinata.pinata.ActivePinataEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PinataHitEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ActivePinataEvent pinata;
    private final Player player;
    private boolean cancelled;

    public PinataHitEvent(ActivePinataEvent pinata, Player player) {
        this.pinata = pinata;
        this.player = player;
    }

    public ActivePinataEvent pinata() { return pinata; }
    public Player player() { return player; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
