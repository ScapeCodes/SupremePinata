package net.scape.project.supremepinata.event;

import net.scape.project.supremepinata.pinata.ActivePinataEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;

public final class PinataCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ActivePinataEvent pinata;
    private final Player finalHitter;

    public PinataCompleteEvent(ActivePinataEvent pinata, Player finalHitter) {
        this.pinata = pinata;
        this.finalHitter = finalHitter;
    }

    public ActivePinataEvent pinata() { return pinata; }
    public Optional<Player> finalHitter() { return Optional.ofNullable(finalHitter); }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
