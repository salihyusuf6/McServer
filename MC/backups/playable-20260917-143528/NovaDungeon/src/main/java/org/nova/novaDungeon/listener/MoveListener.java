package org.nova.novaDungeon.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.nova.novaDungeon.manager.MobManager;
import org.nova.novaDungeon.util.RegionUtil;

import java.util.HashMap;
import java.util.UUID;

public class MoveListener implements Listener {

    private final MobManager mobManager;
    private final HashMap<UUID, Long> cooldown = new HashMap<>();

    public MoveListener(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {

        Player p = e.getPlayer();
        Location loc = p.getLocation();

        if (!loc.getWorld().getName().equalsIgnoreCase("dungeon")) return;

        long now = System.currentTimeMillis();
        if (cooldown.containsKey(p.getUniqueId())) {
            if (now - cooldown.get(p.getUniqueId()) < 1500) return;
        }
        cooldown.put(p.getUniqueId(), now);

        int level = RegionUtil.getRegionLevel(loc);

        if (level > 0) {
            mobManager.trySpawn(p, loc, level);
        }
    }
}