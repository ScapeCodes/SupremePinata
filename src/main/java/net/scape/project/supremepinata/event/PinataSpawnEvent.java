package net.scape.project.supremepinata.event;

import net.scape.project.supremepinata.pinata.PinataType;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PinataSpawnEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final PinataType type;
    private final Location location;
    private boolean cancelled;

    public PinataSpawnEvent(PinataType type, Location location) {
        this.type = type;
        this.location = location;
    }

    public PinataType type() { return type; }
    public Location location() { return location; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
