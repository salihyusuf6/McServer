package org.nova.novaCrates.commands;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.nova.novaCrates.crate.*;
import java.util.*;
public class CrateCommand implements TabExecutor {
    private final CrateManager manager;private final CrateGUI gui;
    public CrateCommand(CrateManager manager){this.manager=manager;gui=new CrateGUI(manager);}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(a.length==0){s.sendMessage("§d✦ NOVA KASALARI §7"+String.join(" • ",manager.names()));s.sendMessage("§7/crate preview <kasa> §8• §7Zindan muhafızlarından anahtar kazan.");return true;}
        String action=a[0].toLowerCase(Locale.ROOT);
        if(action.equals("preview")){if(s instanceof Player p && a.length==2)gui.preview(p,a[1].toLowerCase(Locale.ROOT));else s.sendMessage("/crate preview <kasa>");return true;}
        if(!s.hasPermission("novacrates.admin")){s.sendMessage("§cBu işlem için yetkin yok.");return true;}
        try{
            if(action.equals("give") && a.length>=3){
                Player target=Bukkit.getPlayerExact(a[1]);String name=a[2].toLowerCase(Locale.ROOT);int amount=a.length>3?Integer.parseInt(a[3]):1;
                if(target==null||!manager.exists(name)||amount<1||amount>64){s.sendMessage("§cGeçerli oyuncu, kasa ve 1–64 miktar belirt.");return true;}
                if(target.getInventory().firstEmpty()<0){s.sendMessage("§cOyuncunun çantası dolu.");return true;}
                var key=manager.createKey(name);key.setAmount(amount);target.getInventory().addItem(key);target.saveData();s.sendMessage("§aAnahtar teslim edildi.");return true;
            }
            if(action.equals("set") && a.length==2 && s instanceof Player p){var b=p.getTargetBlockExact(6);if(b==null){s.sendMessage("§cBir bloğa bak.");return true;}manager.setCrate(a[1].toLowerCase(Locale.ROOT),b.getLocation());s.sendMessage("§aKasa tasarımı seçtiğin bloğa yerleştirildi.");return true;}
            if(action.equals("remove") && a.length==2){manager.removeCrate(a[1]);s.sendMessage("§aKasa konumu ve tasarımı kaldırıldı; blok korundu.");return true;}
        }catch(IllegalArgumentException e){s.sendMessage("§cGeçersiz değer: "+e.getMessage());return true;}
        s.sendMessage("§7/crate set <kasa> • remove <kasa> • give <oyuncu> <kasa> [1-64]");return true;
    }
    public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){
        List<String> options=a.length==1?(s.hasPermission("novacrates.admin")?List.of("preview","set","remove","give"):List.of("preview")):a.length==2&&!a[0].equals("give")?new ArrayList<>(manager.names()):a.length==3&&a[0].equals("give")?new ArrayList<>(manager.names()):List.of();
        return options.stream().filter(v->v.startsWith(a[a.length-1].toLowerCase(Locale.ROOT))).toList();
    }
}
