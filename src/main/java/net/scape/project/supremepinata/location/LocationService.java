package net.scape.project.supremepinata.location;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class LocationService {
    private final JavaPlugin plugin;
    private final File file;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, StoredLocation> locations = new ConcurrentHashMap<>();

    public LocationService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/locations.yml");
    }

    public void reload() {
        locations.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("locations");
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            ConfigurationSection loc = section.getConfigurationSection(name);
            if (loc == null) continue;
            locations.put(name.toLowerCase(), new StoredLocation(loc.getString("world", "world"), loc.getDouble("x"), loc.getDouble("y"), loc.getDouble("z"), (float) loc.getDouble("yaw"), (float) loc.getDouble("pitch")));
        }
    }

    public void set(String name, Location location) throws IOException {
        locations.put(name.toLowerCase(), StoredLocation.from(location));
        save();
    }

    public boolean delete(String name) throws IOException {
        boolean removed = locations.remove(name.toLowerCase()) != null;
        save();
        return removed;
    }

    public Optional<Location> get(String name) {
        StoredLocation location = locations.get(name.toLowerCase());
        return location == null ? Optional.empty() : location.toBukkit();
    }

    public Optional<Location> random(List<String> names) {
        List<Location> candidates = new ArrayList<>();
        for (String name : names) get(name).ifPresent(candidates::add);
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    public List<String> names() {
        return locations.keySet().stream().sorted().toList();
    }

    private void save() throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) plugin.getLogger().warning("Could not create data directory.");
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, StoredLocation> entry : locations.entrySet()) {
            String path = "locations." + entry.getKey() + ".";
            StoredLocation loc = entry.getValue();
            cfg.set(path + "world", loc.world());
            cfg.set(path + "x", loc.x());
            cfg.set(path + "y", loc.y());
            cfg.set(path + "z", loc.z());
            cfg.set(path + "yaw", loc.yaw());
            cfg.set(path + "pitch", loc.pitch());
        }
        cfg.save(file);
    }
}
