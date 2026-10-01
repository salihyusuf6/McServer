package org.nova.novaDungeon.util;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import org.bukkit.Location;
import org.nova.novaDungeon.NovaDungeon;
public class RegionUtil {
    public static int getRegionLevel(Location loc){
        if(loc==null||!loc.getWorld().getName().equals(NovaDungeon.getInstance().getConfig().getString("world","dungeon")))return 0;
        var manager=WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));if(manager==null)return 0;
        int level=0;
        for(var region:manager.getApplicableRegions(BlockVector3.at(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ())))for(int i=1;i<=3;i++)if(region.getId().equalsIgnoreCase("level"+i))level=Math.max(level,i);
        return level;
    }
}
