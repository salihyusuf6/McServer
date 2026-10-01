package org.nova.novaDungeon.manager;

import org.bukkit.Bukkit;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.util.HashMap;
import java.util.UUID;

public class BossbarManager {

    private final HashMap<UUID, BossBar> bars = new HashMap<>();

    public void show(Player p, Zombie z) {

        BossBar bar = bars.computeIfAbsent(p.getUniqueId(),
                k -> Bukkit.createBossBar("Mob HP", BarColor.RED, BarStyle.SOLID));

        bar.addPlayer(p);

        double max = z.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double hp = z.getHealth();

        bar.setTitle(z.getCustomName() + " §7" + (int) hp + "/" + (int) max);
        bar.setProgress(Math.max(0, hp / max));
    }
}