package org.nova.novaDungeon.manager;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.attribute.Attribute;
import org.nova.novaDungeon.NovaDungeon;
import java.util.*;
public class BossbarManager {
    private final Map<UUID,BossBar> bars=new HashMap<>();private final Map<UUID,UUID> targets=new HashMap<>();private final Map<UUID,Long> expiry=new HashMap<>();
    public void show(Player p,Zombie z){
        BossBar bar=bars.computeIfAbsent(p.getUniqueId(),id->Bukkit.createBossBar("",BarColor.PURPLE,BarStyle.SEGMENTED_10));bar.addPlayer(p);targets.put(p.getUniqueId(),z.getUniqueId());expiry.put(p.getUniqueId(),System.currentTimeMillis()+6000);update(p,z,bar);
    }
    private void update(Player p,Zombie z,BossBar bar){double max=z.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();bar.setTitle(z.getCustomName()+" §f"+(int)Math.ceil(z.getHealth())+"/"+(int)max);bar.setProgress(Math.max(0,Math.min(1,z.getHealth()/max)));}
    public void tick(){for(UUID id:List.copyOf(bars.keySet())){Player p=Bukkit.getPlayer(id);Entity e=Bukkit.getEntity(targets.get(id));if(p==null||!(e instanceof Zombie z)||z.isDead()||!p.getWorld().equals(z.getWorld())||p.getLocation().distanceSquared(z.getLocation())>900||expiry.get(id)<System.currentTimeMillis())hide(id);else update(p,z,bars.get(id));}}
    public void hide(UUID id){BossBar b=bars.remove(id);if(b!=null)b.removeAll();targets.remove(id);expiry.remove(id);}
    public void clear(){for(UUID id:List.copyOf(bars.keySet()))hide(id);}
}
