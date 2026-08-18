package net.scape.project.supremepinata.event;

import net.scape.project.supremepinata.pinata.ActivePinataEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PinataEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ActivePinataEvent pinata;
    private final String reason;

    public PinataEndEvent(ActivePinataEvent pinata, String reason) {
        this.pinata = pinata;
        this.reason = reason;
    }

    public ActivePinataEvent pinata() { return pinata; }
    public String reason() { return reason; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
