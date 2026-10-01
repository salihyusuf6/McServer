package org.nova.novaCrates.crate;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.nova.novaCrates.NovaCrates;
import java.util.*;

public class CrateGUI {
    public static final class Holder implements InventoryHolder {
        private Inventory inventory;
        public Inventory getInventory(){return inventory;}
    }
    private final CrateManager manager;
    public CrateGUI(CrateManager manager){this.manager=manager;}
    private Inventory inventory(int size,String title){Holder h=new Holder();h.inventory=Bukkit.createInventory(h,size,title);return h.inventory;}
    public void open(Player p,String crate){
        String winner=manager.open(p,crate);if(winner==null)return;
        Inventory inv=inventory(27,"§0✦ "+ChatColor.stripColor(manager.display(crate)));
        ItemStack border=new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);var meta=border.getItemMeta();meta.setDisplayName("§d✦ Kozmik ödül");border.setItemMeta(meta);
        for(int i=0;i<27;i++)inv.setItem(i,border);
        inv.setItem(4,new ItemStack(Material.NETHER_STAR));inv.setItem(22,new ItemStack(Material.NETHER_STAR));p.openInventory(inv);
        new BukkitRunnable(){int frame;
            public void run(){
                if(!p.isOnline()||p.getOpenInventory().getTopInventory()!=inv){manager.finish(p);cancel();return;}
                if(frame++>=24){
                    for(int i=9;i<18;i++)inv.setItem(i,border);
                    inv.setItem(13,manager.parseItem(winner));p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,.5f,1.3f);manager.finish(p);cancel();return;
                }
                for(int i=9;i<18;i++)inv.setItem(i,manager.parseItem(manager.getRandomReward(crate)));
                p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_HAT,.2f,1.5f);
            }
        }.runTaskTimer(NovaCrates.getInstance(),0,3);
    }
    public void preview(Player p,String crate){
        if(!manager.exists(crate)){p.sendMessage("§cBilinmeyen kasa.");return;}
        Inventory inv=inventory(54,"§0✦ Ödüller • "+ChatColor.stripColor(manager.display(crate)));
        List<String> rewards=manager.rewards(crate);int total=rewards.stream().mapToInt(manager::weight).sum();int slot=0;
        for(String r:rewards){if(slot>=54)break;ItemStack item=manager.parseItem(r);var meta=item.getItemMeta();meta.setLore(List.of("§7Olasılık: §e"+String.format(Locale.ROOT,"%.1f",100.0*manager.weight(r)/total)+"%","§8Önizleme • Eşya alınamaz"));item.setItemMeta(meta);inv.setItem(slot++,item);}
        p.openInventory(inv);
    }
}
