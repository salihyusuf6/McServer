package org.nova.cosmos;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** One atomic file per player. A failed write must never award a reward. */
public final class ProgressStore {
    private final Path directory;
    private final Map<UUID, Progress> cache = new HashMap<>();
    public ProgressStore(Path directory) throws IOException {
        this.directory = directory; Files.createDirectories(directory);
    }
    public Progress get(UUID id) throws Exception {
        Progress p = cache.get(id);
        if (p != null) return p;
        Path file = directory.resolve(id + ".yml");
        p = new Progress();
        if (Files.exists(file)) {
            YamlConfiguration y = new YamlConfiguration();
            y.load(file.toFile()); // Invalid data fails closed, never silently resets a wallet.
            p.dust = Math.max(0, y.getLong("dust")); p.started = y.getBoolean("started");
            p.pendingGear = y.getString("delivery.gear", ""); p.deliveryId = y.getString("delivery.id", "");
            p.day = y.getString("day", ""); p.eventId = y.getString("event.id", "");
            p.eventCount = y.getInt("event.count"); p.eventClaimed = y.getBoolean("event.claimed");
            for (int i = 0; i < 3; i++) {
                p.counts[i] = y.getInt("daily." + i + ".count");
                p.claimed[i] = y.getBoolean("daily." + i + ".claimed");
            }
        }
        cache.put(id, p); return p;
    }
    public void save(UUID id, Progress p) throws IOException {
        YamlConfiguration y = new YamlConfiguration();
        y.set("dust", p.dust); y.set("started", p.started); y.set("day", p.day);
        y.set("delivery.gear", p.pendingGear); y.set("delivery.id", p.deliveryId);
        y.set("event.id", p.eventId); y.set("event.count", p.eventCount); y.set("event.claimed", p.eventClaimed);
        for (int i = 0; i < 3; i++) {
            y.set("daily." + i + ".count", p.counts[i]); y.set("daily." + i + ".claimed", p.claimed[i]);
        }
        Path tmp = directory.resolve(id + ".tmp");
        Files.writeString(tmp, y.saveToString(), StandardCharsets.UTF_8);
        Files.move(tmp, directory.resolve(id + ".yml"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        cache.put(id, p);
    }
    public void flush() throws IOException {
        for (var e : new ArrayList<>(cache.entrySet())) save(e.getKey(), e.getValue());
    }
    public void release(UUID id) throws IOException {
        Progress p = cache.get(id);
        if (p != null) { save(id, p); cache.remove(id); }
    }
}
