package org.nova.novaDungeon.manager;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.nova.novaDungeon.NovaDungeon;

import java.util.*;

public class MobManager {

    private final Random random = new Random();

    private final Map<Chunk, Set<UUID>> chunkMobs = new HashMap<>();

    private final int MAX_PER_CHUNK = 5;

    public void trySpawn(Player player, Location loc, int level) {

        Chunk chunk = loc.getChunk();

        chunkMobs.putIfAbsent(chunk, new HashSet<>());

        if (chunkMobs.get(chunk).size() >= MAX_PER_CHUNK) return;

        if (random.nextInt(100) > 40) return;

        Location spawnLoc = loc.clone().add(
                random.nextInt(6) - 3,
                0,
                random.nextInt(6) - 3
        );

        if (spawnLoc.distance(loc) < 3) return;

        spawnMob(chunk, spawnLoc, level);
    }

    private void spawnMob(Chunk chunk, Location loc, int level) {

        Zombie z = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);

        z.setCustomNameVisible(true);
        z.setFireTicks(0);
        z.setRemoveWhenFarAway(false);

        switch (level) {
            case 1:
                z.setCustomName("§aLevel 1 Zombie");
                setStats(z, 20);
                break;
            case 2:
                z.setCustomName("§eLevel 2 Zombie");
                setStats(z, 40);
                break;
            case 3:
                z.setCustomName("§cLevel 3 Zombie");
                setStats(z, 60);
                break;
        }

        chunkMobs.get(chunk).add(z.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!z.isDead()) z.remove();
                chunkMobs.get(chunk).remove(z.getUniqueId());
            }
        }.runTaskLater(NovaDungeon.getInstance(), 20 * 30);
    }

    private void setStats(Zombie z, double hp) {
        z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
        z.setHealth(hp);
    }

    public boolean isDungeonMob(Entity e) {
        for (Set<UUID> set : chunkMobs.values()) {
            if (set.contains(e.getUniqueId())) return true;
        }
        return false;
    }

    public void remove(Entity e) {
        for (Set<UUID> set : chunkMobs.values()) {
            set.remove(e.getUniqueId());
        }
    }
}