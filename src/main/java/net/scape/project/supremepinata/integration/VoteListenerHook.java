package net.scape.project.supremepinata.integration;

import net.scape.project.supremepinata.trigger.VotePartyService;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Dependency-free bridge for public vote plugins using the standard Votifier event API.
 * NuVotifier and VotifierPlus both expose com.vexsoftware.votifier.model.VotifierEvent.
 */
public final class VoteListenerHook implements Listener {
    private static final String VOTIFIER_EVENT_CLASS = "com.vexsoftware.votifier.model.VotifierEvent";

    private final JavaPlugin plugin;
    private final VotePartyService votes;

    public VoteListenerHook(JavaPlugin plugin, VotePartyService votes) {
        this.plugin = plugin;
        this.votes = votes;
    }

    @SuppressWarnings("unchecked")
    public void register() {
        try {
            Class<?> eventClass = Class.forName(VOTIFIER_EVENT_CLASS, false, plugin.getClass().getClassLoader());
            if (!Event.class.isAssignableFrom(eventClass)) {
                plugin.getLogger().warning("Found " + VOTIFIER_EVENT_CLASS + " but it is not a Bukkit event.");
                return;
            }
            Bukkit.getPluginManager().registerEvent((Class<? extends Event>) eventClass, this, EventPriority.MONITOR, new VoteEventExecutor(), plugin, true);
            plugin.getLogger().info("Vote hook enabled for NuVotifier/VotifierPlus.");
        } catch (ClassNotFoundException ignored) {
            plugin.getLogger().info("NuVotifier/VotifierPlus not found; vote hook disabled.");
        }
    }

    private final class VoteEventExecutor implements EventExecutor {
        @Override
        public void execute(Listener listener, Event event) throws EventException {
            String voter = voter(event);
            votes.addVote(voter == null || voter.isBlank() ? "Someone" : voter);
        }

        private String voter(Event event) throws EventException {
            try {
                Method getVote = event.getClass().getMethod("getVote");
                Object vote = getVote.invoke(event);
                if (vote == null) return null;
                Method getUsername = vote.getClass().getMethod("getUsername");
                Object username = getUsername.invoke(vote);
                return username == null ? null : username.toString();
            } catch (ReflectiveOperationException ex) {
                throw new EventException(ex);
            }
        }
    }
}
