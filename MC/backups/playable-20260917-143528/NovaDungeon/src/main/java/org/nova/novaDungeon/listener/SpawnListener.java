package org.nova.novaDungeon.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class SpawnListener implements Listener {

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {

        if (!e.getLocation().getWorld().getName().equalsIgnoreCase("dungeon")) return;

        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            e.setCancelled(true);
        }
    }
}