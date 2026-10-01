package org.nova.novaDungeon.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;

public class FireFixListener implements Listener {

    @EventHandler
    public void onBurn(EntityCombustEvent e) {

        if (e.getEntity().getWorld().getName().equalsIgnoreCase("dungeon")) {
            e.setCancelled(true);
        }
    }
}