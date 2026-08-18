package net.scape.project.supremepinata.event;

import net.scape.project.supremepinata.reward.Reward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PinataRewardEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Reward reward;
    private final String pool;
    private boolean cancelled;

    public PinataRewardEvent(Player player, Reward reward, String pool) {
        this.player = player;
        this.reward = reward;
        this.pool = pool;
    }

    public Player player() { return player; }
    public Reward reward() { return reward; }
    public String pool() { return pool; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
