package org.nova.novaDungeon;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.nova.cosmos.NovaCosmos;
import org.nova.novaDungeon.listener.DeathListener;
import org.nova.novaDungeon.manager.*;
import org.nova.novaDungeon.util.RegionUtil;
import java.util.*;

public final class NovaDungeon extends JavaPlugin implements Listener,TabExecutor {
    private static NovaDungeon instance;private MobManager mobs;private BossbarManager bars;private DungeonWorld generatedWorld;
    private final Map<UUID,Long> travel=new HashMap<>();
    public static NovaDungeon getInstance(){return instance;}
    public MobManager getMobManager(){return mobs;}
    public BossbarManager getBossbarManager(){return bars;}
    public NovaCosmos cosmos(){return (NovaCosmos)Bukkit.getPluginManager().getPlugin("NovaCosmos");}
    private NamespacedKey key(String name){return new NamespacedKey(this,name);}
    public int kills(Player p){return p.getPersistentDataContainer().getOrDefault(key("kills"),PersistentDataType.INTEGER,0);}
    public int tierCount(){return Math.min(5,getConfig().getConfigurationSection("tiers").getKeys(false).size());}
    public DungeonWorld generatedWorld(){return generatedWorld;}
    public int unlocked(Player p){int tier=1;for(int i=2;i<=tierCount();i++)if(kills(p)>=getConfig().getInt("tiers."+i+".unlock-kills",i==2?15:40))tier=i;return tier;}
    public boolean eligible(Player p,int tier){return p.getGameMode()==GameMode.SURVIVAL&&tier>=1&&tier<=tierCount()&&tier<=unlocked(p)&&(!getConfig().getBoolean("require-island",true)||cosmos().hasIsland(p));}
    public void recordKill(Player p){int before=unlocked(p);p.getPersistentDataContainer().set(key("kills"),PersistentDataType.INTEGER,Math.min(100000000,kills(p)+1));p.saveData();if(unlocked(p)>before){p.sendMessage("§a✦ Yeni zindan bölgesi açıldı! /zindan");p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,.7f,1);}}
    public boolean dungeon(World w){return w!=null&&w.getName().equals(getConfig().getString("world","dungeon"));}
    @Override public void onEnable(){instance=this;saveDefaultConfig();mobs=new MobManager();bars=new BossbarManager();generatedWorld=new DungeonWorld(this);Bukkit.getScheduler().runTask(this,generatedWorld::initialize);Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getPluginManager().registerEvents(new DeathListener(),this);for(String cmd:List.of("zindan","dungeonadmin")){getCommand(cmd).setExecutor(this);getCommand(cmd).setTabCompleter(this);}Bukkit.getScheduler().runTaskTimer(this,mobs::tick,60,40);Bukkit.getScheduler().runTaskTimer(this,bars::tick,20,5);}
    @Override public void onDisable(){if(generatedWorld!=null)generatedWorld.close();if(mobs!=null)mobs.clear();if(bars!=null)bars.clear();for(Player p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory().getHolder() instanceof Menu)p.closeInventory();}
    public boolean safe(Location l){
        var feet=l.getBlock();var floor=feet.getRelative(0,-1,0);var head=feet.getRelative(0,1,0);
        return feet.isPassable()&&!feet.isLiquid()&&head.isPassable()&&!head.isLiquid()&&floor.getType().isSolid()&&!Set.of(Material.MAGMA_BLOCK,Material.CAMPFIRE,Material.SOUL_CAMPFIRE,Material.CACTUS,Material.FIRE,Material.SOUL_FIRE,Material.SWEET_BERRY_BUSH,Material.POWDER_SNOW).contains(floor.getType())&&feet.getType()!=Material.FIRE&&feet.getType()!=Material.SOUL_FIRE&&feet.getType()!=Material.POWDER_SNOW;
    }
    public Location entrance(int tier){
        if(generatedWorld!=null&&getConfig().getBoolean("generated-world",false)&&!generatedWorld.ready())return null;
        World w=Bukkit.getWorld(getConfig().getString("world","dungeon"));if(w==null)return null;
        Location configured=getConfig().getLocation("entrances."+tier);if(configured!=null&&dungeon(configured.getWorld())&&safe(configured)&&RegionUtil.getRegionLevel(configured)==tier)return configured;
        if(generatedWorld!=null&&getConfig().getBoolean("generated-world",false))return generatedWorld.entrance(tier);
        var manager=WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w));if(manager==null)return null;var region=manager.getRegion("level"+tier);if(region==null)return null;
        var min=region.getMinimumPoint();var max=region.getMaximumPoint();int cx=(min.x()+max.x())/2,cz=(min.z()+max.z())/2;
        for(int r=0;r<=16;r++)for(int x=cx-r;x<=cx+r;x++)for(int z=cz-r;z<=cz+r;z++){
            if(x<min.x()||x>max.x()||z<min.z()||z>max.z())continue;
            for(int y=Math.max(w.getMinHeight()+1,min.y());y<=Math.min(max.y(),min.y()+12);y++){Location l=new Location(w,x+.5,y,z+.5);if(RegionUtil.getRegionLevel(l)==tier&&safe(l))return l;}
        }return null;
    }
    private void remember(Player p){Location l=p.getLocation();p.getPersistentDataContainer().set(key("return"),PersistentDataType.STRING,l.getWorld().getUID()+","+l.getX()+","+l.getY()+","+l.getZ()+","+l.getYaw()+","+l.getPitch());p.saveData();}
    private Location returnLocation(Player p){
        try{String[] v=p.getPersistentDataContainer().getOrDefault(key("return"),PersistentDataType.STRING,"").split(",");World w=Bukkit.getWorld(UUID.fromString(v[0]));if(w!=null&&!dungeon(w)){Location l=new Location(w,Double.parseDouble(v[1]),Double.parseDouble(v[2]),Double.parseDouble(v[3]),Float.parseFloat(v[4]),Float.parseFloat(v[5]));if(safe(l))return l;}}catch(RuntimeException ignored){}
        return Bukkit.getWorlds().stream().filter(w->!dungeon(w)).findFirst().orElseThrow().getSpawnLocation();
    }
    private void enter(Player p,int tier){
        if(tier<1||tier>tierCount()||!eligible(p,tier)){p.sendMessage("§cÖnce /is ile ada oluştur; hayatta kalma modunda oyna. Bölge gereksinimlerini /zindan menüsünde görebilirsin.");return;}
        Location dest=entrance(tier);if(dest==null){p.sendMessage("§cBu bölgenin güvenli girişi hazır değil. Yönetici: /dungeonadmin durum");return;}
        delayed(p,()->{if(!eligible(p,tier)||!safe(dest))return;if(!dungeon(p.getWorld()))remember(p);if(p.teleport(dest)){p.sendMessage("§d✦ "+getConfig().getString("tiers."+tier+".name")+" §7• Çıkış: /zindan cik");p.playSound(dest,Sound.BLOCK_PORTAL_TRAVEL,.2f,1.4f);}});
    }
    private void hub(Player p){
        if(!eligible(p,1)){p.sendMessage("§cÖnce /is ile ada oluştur ve hayatta kalma moduna geç.");return;}
        if(generatedWorld==null||!generatedWorld.ready()||generatedWorld.world()==null){p.sendMessage("§eZindan dünyası hazırlanıyor.");return;}
        delayed(p,()->{if(!eligible(p,1))return;if(!dungeon(p.getWorld()))remember(p);p.teleport(generatedWorld.hub());p.sendMessage("§b✦ Güvenli merkez. Köprülerden ilerleyerek bölgelere ulaş.");});
    }
    private void delayed(Player p,Runnable action){
        if(travel.containsKey(p.getUniqueId())){p.sendMessage("§eIşınlanma zaten hazırlanıyor.");return;}
        long token=System.nanoTime();travel.put(p.getUniqueId(),token);Location origin=p.getLocation();p.closeInventory();p.sendMessage("§b3 saniye hareketsiz kal. Hasar almak ışınlanmayı iptal eder.");
        Bukkit.getScheduler().runTaskLater(this,()->{if(!Objects.equals(travel.get(p.getUniqueId()),token))return;travel.remove(p.getUniqueId());if(p.isOnline()&&!p.isDead()&&p.getWorld().equals(origin.getWorld())&&p.getLocation().distanceSquared(origin)<.25)action.run();else if(p.isOnline())p.sendMessage("§cHareket ettin, ışınlanma iptal edildi.");},60);
    }
    private static class Menu implements InventoryHolder {Inventory inv;public Inventory getInventory(){return inv;}}
    private ItemStack icon(Material m,String title,String...lore){var item=new ItemStack(m);var meta=item.getItemMeta();meta.setDisplayName(title);meta.setLore(Arrays.asList(lore));item.setItemMeta(meta);return item;}
    private void menu(Player p){Menu h=new Menu();h.inv=Bukkit.createInventory(h,27,"§0✦ NOVA • Yıldız Zindanı");for(int i=0;i<27;i++)h.inv.setItem(i,icon(Material.BLACK_STAINED_GLASS_PANE," "));
        for(int t=1;t<=tierCount();t++){boolean open=unlocked(p)>=t;h.inv.setItem(9+t,icon(open?Material.IRON_SWORD:Material.BARRIER,(open?"§b":"§c")+getConfig().getString("tiers."+t+".name"),"§7Gerekli av: "+getConfig().getInt("tiers."+t+".unlock-kills"),"§7Can: "+getConfig().getInt("tiers."+t+".health")+" • Hasar: "+getConfig().getInt("tiers."+t+".damage"),"§bAv başına "+getConfig().getInt("tiers."+t+".dust")+" Yıldız Tozu","§d12 avda bir muhafız • 6 kat toz + anahtar",open?"§eGirmek için tıkla":"§8Önce alt bölgede ilerle"));}
        h.inv.setItem(4,icon(Material.NETHER_STAR,"§b✦ Güvenli Merkez • Av: "+kills(p),"§eMerkeze gitmek için tıkla.","§7Tozunu /uzay menüsünde ekipmana dönüştür.","§7Zindanda ölümde envanterin korunur.","§7Muhafızın işaretlediği alandan kaç!"));h.inv.setItem(22,icon(Material.OAK_DOOR,"§eGeldiğin yere dön","§7/zindan cik • 3 saniye bekleme"));p.openInventory(h.inv);
    }
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(c.getName().equals("dungeonadmin")){
            if(!s.hasPermission("novadungeon.admin")){s.sendMessage("§cYetkin yok.");return true;}
            if(a.length==2&&a[0].equalsIgnoreCase("setspawn")&&s instanceof Player p){try{int t=Integer.parseInt(a[1]);if(t<1||t>tierCount()||RegionUtil.getRegionLevel(p.getLocation())!=t||!safe(p.getLocation()))throw new IllegalArgumentException();getConfig().set("entrances."+t,p.getLocation());saveConfig();s.sendMessage("§aGüvenli giriş kaydedildi.");}catch(IllegalArgumentException e){s.sendMessage("§cİlgili level1/2/3/4/5 bölgesinde güvenli zeminde dur.");}return true;}
            for(int t=1;t<=tierCount();t++){Location loc=entrance(t);s.sendMessage("§bBölge "+t+": "+(loc==null?"§cGiriş bulunamadı":"§aHazır "+loc.getBlockX()+", "+loc.getBlockY()+", "+loc.getBlockZ()));}s.sendMessage("§7/dungeonadmin setspawn <1|2|3|4|5>");return true;
        }
        if(!(s instanceof Player p))return true;
        if(a.length==0){menu(p);return true;}
        if(a[0].equalsIgnoreCase("cik")){if(!dungeon(p.getWorld())){p.sendMessage("§7Zindanda değilsin.");return true;}delayed(p,()->p.teleport(returnLocation(p)));return true;}
        if(a[0].equalsIgnoreCase("merkez")){hub(p);return true;}
        if(a[0].equalsIgnoreCase("gir")){try{if(a.length==1)hub(p);else enter(p,Integer.parseInt(a[1]));}catch(NumberFormatException e){p.sendMessage("§c/zindan gir <1|2|3|4|5>");}return true;}
        p.sendMessage("§7/zindan • /zindan gir <1|2|3|4|5> • /zindan cik");return true;
    }
    public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){return a.length==1?(c.getName().equals("dungeonadmin")?List.of("durum","setspawn"):List.of("gir","merkez","cik")):a.length==2?List.of("1","2","3","4","5"):List.of();}
    @EventHandler public void click(InventoryClickEvent e){if(!(e.getView().getTopInventory().getHolder() instanceof Menu))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||!e.isLeftClick()||e.isShiftClick())return;int slot=e.getRawSlot();Bukkit.getScheduler().runTask(this,()->{if(slot>=10&&slot<=14)enter(p,slot-9);else if(slot==4)hub(p);else if(slot==22)p.performCommand("zindan cik");});}
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void hit(EntityDamageByEntityEvent e){
        Entity attacker=e.getDamager();if(attacker instanceof Projectile arrow&&arrow.getShooter() instanceof Entity shooter)attacker=shooter;
        if(dungeon(e.getEntity().getWorld())&&attacker instanceof Player&&e.getEntity() instanceof Player){e.setCancelled(true);return;}
        if(mobs.isDungeonMob(attacker)&&e.getEntity() instanceof Player target){
            if(RegionUtil.getRegionLevel(target.getLocation())!=mobs.level(attacker)||!eligible(target,mobs.level(attacker))){e.setCancelled(true);return;}
            if(e.getDamager() instanceof Projectile)e.setDamage(getConfig().getDouble("tiers."+mobs.level(attacker)+".damage",3));
        }
        if(mobs.isDungeonMob(e.getEntity())){
            if(!(attacker instanceof Player p)||!eligible(p,mobs.level(e.getEntity()))||RegionUtil.getRegionLevel(p.getLocation())!=mobs.level(e.getEntity())){e.setCancelled(true);return;}
            if(e.getEntity() instanceof LivingEntity z)Bukkit.getScheduler().runTask(this,()->{if(!z.isDead())bars.show(p,z);});
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player p&&travel.remove(p.getUniqueId())!=null)p.sendMessage("§cHasar aldın, ışınlanma iptal edildi.");}
    @EventHandler public void move(PlayerMoveEvent e){if(travel.containsKey(e.getPlayer().getUniqueId())&&e.getTo()!=null&&(e.getFrom().getBlockX()!=e.getTo().getBlockX()||e.getFrom().getBlockY()!=e.getTo().getBlockY()||e.getFrom().getBlockZ()!=e.getTo().getBlockZ())){travel.remove(e.getPlayer().getUniqueId());e.getPlayer().sendMessage("§cHareket ettin, ışınlanma iptal edildi.");}}
    @EventHandler(ignoreCancelled=true) public void zoneMove(PlayerMoveEvent e){
        if(e.getTo()==null||!dungeon(e.getTo().getWorld()))return;
        int before=RegionUtil.getRegionLevel(e.getFrom()),after=RegionUtil.getRegionLevel(e.getTo());
        if(after!=before&&after>0&&!eligible(e.getPlayer(),after)&&!e.getPlayer().hasPermission("novadungeon.admin")){
            e.setCancelled(true);e.getPlayer().sendActionBar("§cBu bölge için "+getConfig().getInt("tiers."+after+".unlock-kills")+" av gerekli.");return;
        }
        if(after!=before&&after>0){e.getPlayer().sendTitle("§dBölge "+after,"§f"+getConfig().getString("tiers."+after+".name"),10,45,10);}
        if(e.getTo().getY()<60&&getConfig().getBoolean("generated-world",false)&&generatedWorld.ready()){
            e.getPlayer().setFallDistance(0);e.setTo(generatedWorld.hub());
        }
    }
    @EventHandler(ignoreCancelled=true) public void zoneTeleport(PlayerTeleportEvent e){zoneMove(e);}
    @EventHandler public void quit(PlayerQuitEvent e){travel.remove(e.getPlayer().getUniqueId());bars.hide(e.getPlayer().getUniqueId());}
    @EventHandler public void burn(EntityCombustEvent e){if(mobs.isDungeonMob(e.getEntity()))e.setCancelled(true);}
    @EventHandler public void spawn(CreatureSpawnEvent e){if(dungeon(e.getLocation().getWorld())&&e.getSpawnReason()!=CreatureSpawnEvent.SpawnReason.CUSTOM)e.setCancelled(true);}
    @EventHandler public void breaking(BlockBreakEvent e){if(dungeon(e.getBlock().getWorld())&&!e.getPlayer().hasPermission("novadungeon.admin"))e.setCancelled(true);}
    @EventHandler public void bucket(PlayerBucketEmptyEvent e){if(dungeon(e.getBlock().getWorld())&&!e.getPlayer().hasPermission("novadungeon.admin"))e.setCancelled(true);}
    @EventHandler public void bucket(PlayerBucketFillEvent e){if(dungeon(e.getBlock().getWorld())&&!e.getPlayer().hasPermission("novadungeon.admin"))e.setCancelled(true);}
    @EventHandler public void grief(EntityChangeBlockEvent e){if(dungeon(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler public void placing(BlockPlaceEvent e){if(dungeon(e.getBlock().getWorld())&&!e.getPlayer().hasPermission("novadungeon.admin"))e.setCancelled(true);}
    @EventHandler public void explode(EntityExplodeEvent e){if(dungeon(e.getLocation().getWorld()))e.blockList().clear();}
    @EventHandler public void explode(BlockExplodeEvent e){if(dungeon(e.getBlock().getWorld()))e.blockList().clear();}
    @EventHandler public void death(PlayerDeathEvent e){if(dungeon(e.getEntity().getWorld())){e.setKeepInventory(true);e.getDrops().clear();e.setKeepLevel(true);e.setDroppedExp(0);e.getEntity().getPersistentDataContainer().set(key("respawn"),PersistentDataType.BYTE,(byte)1);}}
    @EventHandler public void respawn(PlayerRespawnEvent e){if(e.getPlayer().getPersistentDataContainer().has(key("respawn"),PersistentDataType.BYTE)){e.setRespawnLocation(returnLocation(e.getPlayer()));e.getPlayer().getPersistentDataContainer().remove(key("respawn"));}}
}
