package org.nova.novaDungeon.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

public class RegionUtil {

    public static int getRegionLevel(Location loc) {

        var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        var manager = container.get(BukkitAdapter.adapt(loc.getWorld()));

        if (manager == null) return 0;

        ApplicableRegionSet regions = manager.getApplicableRegions(
                BlockVector3.at(loc.getX(), loc.getY(), loc.getZ())
        );

        for (ProtectedRegion r : regions) {
            String id = r.getId();

            if (id.equalsIgnoreCase("level1")) return 1;
            if (id.equalsIgnoreCase("level2")) return 2;
            if (id.equalsIgnoreCase("level3")) return 3;
        }

        return 0;
    }
}