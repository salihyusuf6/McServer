package org.nova.novaDungeon.listener;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.nova.novaDungeon.NovaDungeon;
import org.nova.novaCrates.NovaCrates;
import java.util.concurrent.ThreadLocalRandom;
public class DeathListener implements Listener {
    @EventHandler public void death(EntityDeathEvent e){
        NovaDungeon plugin=NovaDungeon.getInstance();var mobs=plugin.getMobManager();if(!mobs.isDungeonMob(e.getEntity()))return;
        int level=mobs.level(e.getEntity());boolean boss=mobs.boss(e.getEntity());mobs.remove(e.getEntity());e.getDrops().clear();e.setDroppedExp(0);
        Player p=e.getEntity().getKiller();if(p==null||!plugin.eligible(p,level))return;
        int dust=plugin.getConfig().getInt("tiers."+level+".dust",level)*(boss?6:1);
        if(!plugin.cosmos().creditDust(p,Math.max(1,Math.min(100000,dust)))){p.sendMessage("§cZindan tozu kaydedilemedi; yöneticiye bildir.");return;}
        plugin.recordKill(p);e.setDroppedExp(level*3);
        p.sendMessage("§b✦ +"+dust+" Yıldız Tozu §8• §7/uzay ile ekipman üret"+(boss?" §d[Muhafız]":""));
        double chance=plugin.getConfig().getDouble("rewards.key-chance",.04);
        if(boss||ThreadLocalRandom.current().nextDouble()<Math.max(0,Math.min(1,chance))){
            String crate=plugin.getConfig().getString("tiers."+level+".crate","vote");
            var key=NovaCrates.getInstance().getCrateManager().createKey(crate);
            if(key!=null){var leftovers=p.getInventory().addItem(key);leftovers.values().forEach(item->{var drop=p.getWorld().dropItemNaturally(p.getLocation(),item);drop.setOwner(p.getUniqueId());});p.saveData();p.sendMessage("§d✦ "+crate+" kasa anahtarı kazandın!"+(leftovers.isEmpty()?"":" §eÇantan dolu; anahtar ayaklarının altında."));}
        }
    }
}
