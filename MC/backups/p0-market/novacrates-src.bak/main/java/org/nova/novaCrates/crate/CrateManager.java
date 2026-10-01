package org.nova.novaCrates.crate;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.nova.cosmos.NovaCosmos;
import org.nova.novaCrates.NovaCrates;

public class CrateManager {
    private final NovaCrates plugin;
    private final Map<String, Location> locations = new LinkedHashMap<>();
    private final Random random = new Random();
    private final Set<UUID> opening = new HashSet<>();
    public CrateManager(NovaCrates plugin) { this.plugin = plugin; loadLocations(); }
    public Map<String, Location> locations() { return Collections.unmodifiableMap(locations); }
    public Set<String> names() { return plugin.getConfig().getConfigurationSection("crates").getKeys(false); }
    public String display(String name) { return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("crates."+name+".display", name+" Kasası")); }
    public boolean exists(String name) { return plugin.getConfig().isConfigurationSection("crates."+name); }
    public boolean busy(Player p) { return opening.contains(p.getUniqueId()); }
    public void finish(Player p) { opening.remove(p.getUniqueId()); }
    public void setCrate(String name, Location loc) {
        if (!exists(name)) throw new IllegalArgumentException("Bilinmeyen kasa");
        if (getCrateAt(loc) != null && !name.equals(getCrateAt(loc))) throw new IllegalArgumentException("Bu blok zaten bir kasaya ait.");
        locations.put(name, loc.getBlock().getLocation());
        plugin.getConfig().set("crate-locations."+name, loc.getWorld().getName()+","+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ());
        plugin.saveConfig(); plugin.visuals().refresh();
    }
    public void removeCrate(String name) {
        locations.remove(name); plugin.getConfig().set("crate-locations."+name, null); plugin.saveConfig(); plugin.visuals().refresh();
    }
    public String getCrateAt(Location loc) {
        for (var e:locations.entrySet()) if (e.getValue().equals(loc.getBlock().getLocation())) return e.getKey();
        return null;
    }
    public void loadLocations() {
        locations.clear();
        var section=plugin.getConfig().getConfigurationSection("crate-locations");
        if (section == null) return;
        for(String name:section.getKeys(false)) try {
            String[] p=section.getString(name, "").split(","); World w=Bukkit.getWorld(p[0]);
            if (w == null) continue;
            locations.put(name,new Location(w,Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3])));
        } catch(RuntimeException e) { plugin.getLogger().warning("Geçersiz kasa konumu: "+name); }
    }
    public ItemStack createKey(String name) {
        if (!exists(name)) return null;
        ItemStack item=new ItemStack(Material.TRIPWIRE_HOOK); var meta=item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("crates."+name+".key-name",name+" Anahtarı")));
        meta.setLore(List.of("§7"+ChatColor.stripColor(display(name))+" için kullan.","§8Kasaya sağ tıkla • Tek kullanımlık"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin,"crate_type"),PersistentDataType.STRING,name);
        item.setItemMeta(meta); return item;
    }
    public boolean isKey(ItemStack item,String crate) {
        return item!=null && item.getType()==Material.TRIPWIRE_HOOK && item.hasItemMeta() && crate.equals(item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin,"crate_type"),PersistentDataType.STRING));
    }
    public List<String> rewards(String crate) {
        List<String> result=new ArrayList<>();
        for(String r:plugin.getConfig().getStringList("crates."+crate+".rewards")) try {
            String[] p=r.split(":");
            if (weight(r)<=0 || weight(r)>100000) continue;
            if(p[0].equals("ITEM") && p.length==4 && Material.valueOf(p[1]).isItem() && Material.valueOf(p[1])!=Material.AIR && Integer.parseInt(p[2])>0 && Integer.parseInt(p[2])<=Material.valueOf(p[1]).getMaxStackSize()) result.add(r);
            else if((p[0].equals("MONEY")||p[0].equals("DUST")) && p.length==3 && Integer.parseInt(p[1])>0 && Integer.parseInt(p[1])<=100000) result.add(r);
        } catch(RuntimeException ignored) { }
        return result;
    }
    public int weight(String r) { String[] p=r.split(":"); return Integer.parseInt(p[p.length-1]); }
    public String getRandomReward(String crate) {
        List<String> list=rewards(crate); int total=list.stream().mapToInt(this::weight).sum();
        if(total<=0) throw new IllegalArgumentException("Kasanın geçerli ödülü yok.");
        int n=random.nextInt(total); for(String r:list) { n-=weight(r); if(n<0)return r; } throw new IllegalStateException();
    }
    public ItemStack parseItem(String r) {
        String[] p=r.split(":");
        if(p[0].equals("ITEM")) return new ItemStack(Material.valueOf(p[1]),Integer.parseInt(p[2]));
        ItemStack item=new ItemStack(p[0].equals("DUST")?Material.NETHER_STAR:Material.PAPER);var m=item.getItemMeta();
        m.setDisplayName("§b"+p[1]+(p[0].equals("DUST")?" Yıldız Tozu":" Para"));item.setItemMeta(m);return item;
    }
    public int parseMoney(String reward) {return Integer.parseInt(reward.split(":")[1]);}
    /** Reward is delivered synchronously; the following animation never awards items. */
    public String open(Player player,String crate) {
        if(busy(player)||player.getGameMode()!=GameMode.SURVIVAL) {player.sendMessage("§cKasaları hayatta kalma modunda, sırayla aç.");return null;}
        if(player.getInventory().firstEmpty()<0) {player.sendMessage("§cÖnce çantanda bir boş yer aç.");return null;}
        int keySlot=-1;
        for(int i=0;i<36;i++) if(isKey(player.getInventory().getItem(i),crate)){keySlot=i;break;}
        if(keySlot<0){player.sendMessage("§cBu kasa için anahtarın yok. Zindan muhafızlarından kazanabilirsin.");return null;}
        String reward;try {reward=getRandomReward(crate);}catch(RuntimeException e){player.sendMessage("§cKasa ödülleri hazır değil; yöneticiye bildir.");return null;}
        String[] parts=reward.split(":");ItemStack key=player.getInventory().getItem(keySlot);ItemStack original=key.clone();
        opening.add(player.getUniqueId());key.setAmount(key.getAmount()-1);
        boolean success=false;
        try {
            if(parts[0].equals("ITEM")){player.getInventory().addItem(parseItem(reward));success=true;}
            else if(parts[0].equals("MONEY")) success=NovaCrates.econ!=null && NovaCrates.econ.depositPlayer(player,parseMoney(reward)).transactionSuccess();
            else if(parts[0].equals("DUST")) success=((NovaCosmos)Bukkit.getPluginManager().getPlugin("NovaCosmos")).creditDust(player,parseMoney(reward));
        } catch(RuntimeException e){plugin.getLogger().severe("Kasa teslimat hatası: "+e.getMessage());}
        if(!success){player.getInventory().setItem(keySlot,original);finish(player);player.sendMessage("§cÖdül verilemedi; anahtarın korunuyor.");return null;}
        player.saveData();
        player.sendMessage("§b✦ §f"+display(crate)+" §7ödülün teslim edildi: §e"+rewardLabel(reward));
        return reward;
    }
    public String rewardLabel(String r){String[] p=r.split(":");return p[0].equals("ITEM")?p[2]+" × "+p[1]:p[1]+(p[0].equals("DUST")?" Yıldız Tozu":" Para");}
}
