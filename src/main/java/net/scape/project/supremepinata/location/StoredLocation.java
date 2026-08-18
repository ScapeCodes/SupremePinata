package net.scape.project.supremepinata.location;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {
    public static StoredLocation from(Location location) {
        return new StoredLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Optional<Location> toBukkit() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) return Optional.empty();
        return Optional.of(new Location(bukkitWorld, x, y, z, yaw, pitch));
    }
}
