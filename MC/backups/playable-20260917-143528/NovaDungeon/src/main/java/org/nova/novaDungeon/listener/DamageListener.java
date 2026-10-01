package org.nova.novaDungeon.listener;

import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.nova.novaDungeon.NovaDungeon;

public class DamageListener implements Listener {

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof Zombie)) return;

        Player p = (Player) e.getDamager();
        Zombie z = (Zombie) e.getEntity();

        NovaDungeon.getInstance().getBossbarManager().show(p, z);
    }
}