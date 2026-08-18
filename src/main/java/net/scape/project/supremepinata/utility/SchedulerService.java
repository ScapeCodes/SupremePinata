package net.scape.project.supremepinata.utility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class SchedulerService {
    private final Plugin plugin;
    private final boolean folia;

    public SchedulerService(Plugin plugin) {
        this.plugin = plugin;
        this.folia = hasMethod(Bukkit.class, "getGlobalRegionScheduler");
        if (folia) plugin.getLogger().info("Folia scheduler support enabled.");
    }

    public boolean isFolia() {
        return folia;
    }

    public Object runEntityRepeating(Entity entity, Runnable runnable, long delay, long period) {
        if (!folia) return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            Method method = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
            return method.invoke(scheduler, plugin, (Consumer<Object>) task -> runnable.run(), null, delay, period);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().severe("Folia entity scheduler failed. Disabling repeating task to avoid unsafe global scheduling: " + ex.getMessage());
            return null;
        }
    }

    public void runAtLocation(Location location, Runnable runnable) {
        if (!folia) {
            Bukkit.getScheduler().runTask(plugin, runnable);
            return;
        }
        try {
            Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Method method = scheduler.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class);
            method.invoke(scheduler, plugin, location, runnable);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().severe("Folia region scheduler failed. Skipping unsafe global fallback: " + ex.getMessage());
        }
    }

    public void runGlobal(Runnable runnable) {
        if (!folia) {
            Bukkit.getScheduler().runTask(plugin, runnable);
            return;
        }
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method method = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
            method.invoke(scheduler, plugin, runnable);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().severe("Folia global scheduler failed. Skipping unsafe global fallback: " + ex.getMessage());
        }
    }

    public void cancel(Object task) {
        if (task == null) return;
        if (task instanceof BukkitTask bukkitTask) {
            bukkitTask.cancel();
            return;
        }
        try {
            task.getClass().getMethod("cancel").invoke(task);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    private boolean hasMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) if (method.getName().equals(name)) return true;
        return false;
    }
}
