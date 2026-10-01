package org.nova.qa;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.nova.novaCrates.*;
import org.nova.novaDungeon.*;
import org.nova.cosmos.*;
import java.util.*;

/** Installed exclusively on the isolated test server. Never ship to production. */
public final class NovaProbe extends JavaPlugin {
    void check(boolean ok,String name){if(!ok)throw new AssertionError(name);getLogger().info("PASS: "+name);}
    void checkPath(World w,int x,int z){if(!w.getBlockAt(x,80,z).getType().isSolid()||!w.getBlockAt(x,81,z).isPassable()||!w.getBlockAt(x,82,z).isPassable())throw new AssertionError("Broken corridor "+x+","+z);}
    @Override public void onEnable(){getCommand("novaprobe").setExecutor(this);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player p)||!p.isOp())return true;
        try {
            var crates=NovaCrates.getInstance().getCrateManager();var dungeon=NovaDungeon.getInstance();var cosmos=(NovaCosmos)Bukkit.getPluginManager().getPlugin("NovaCosmos");
            if(args.length>0&&args[0].equals("autospawn")){
                p.setInvulnerable(true);p.setGameMode(GameMode.SURVIVAL);
                new org.bukkit.scheduler.BukkitRunnable(){int step=-1;int[] tiers={3,1,2,3,4,5};
                    public void run(){try{
                        if(step>=0){int level=tiers[step];var found=p.getWorld().getEntitiesByClass(Mob.class).stream().filter(m->dungeon.getMobManager().level(m)==level).toList();check(!found.isEmpty(),"AUTOMATIC spawn tier "+level+(step==0?" normal player 54 kills":" OP with zero kills"));check(found.get(0).getType()==EntityType.valueOf(dungeon.getConfig().getString("tiers."+level+".mob")),"Automatic correct mob type "+level);}
                        step++;dungeon.getMobManager().clear();if(step==tiers.length){p.setOp(true);p.setInvulnerable(false);p.teleport(dungeon.generatedWorld().hub());p.sendMessage("PROBE_AUTO_OK");cancel();return;}
                        p.setOp(true);p.getPersistentDataContainer().set(new NamespacedKey(dungeon,"kills"),PersistentDataType.INTEGER,step==0?54:0);p.getPersistentDataContainer().set(new NamespacedKey(dungeon,"boss-round"),PersistentDataType.INTEGER,100);p.teleport(dungeon.entrance(tiers[step]));if(step==0)p.setOp(false);
                    }catch(Throwable t){p.setOp(true);p.setInvulnerable(false);p.sendMessage("PROBE_FAIL "+t);cancel();}}
                }.runTaskTimer(this,1,100);return true;
            }
            if(args.length>0&&args[0].equals("quiz")){
                p.getInventory().clear();dungeon.mathQuiz().start();p.sendMessage("PROBE_ANSWER "+dungeon.mathQuiz().current().answer);return true;
            }
            if(args.length>0&&args[0].equals("quizfull")){
                p.getInventory().clear();for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Material.STONE,64));dungeon.mathQuiz().start();p.sendMessage("PROBE_ANSWER "+dungeon.mathQuiz().current().answer);return true;
            }
            if(args.length>0&&args[0].equals("quizroom")){p.getInventory().setItem(0,null);p.sendMessage("PROBE_ROOM");return true;}
            if(args.length>0&&args[0].equals("map")){
                var map=dungeon.generatedWorld();check(map.ready()&&map.world()!=null,"Generated dungeon world ready");
                check(dungeon.tierCount()==5,"Five playable tiers configured");
                World w=map.world();
                for(int z=0;z<=328;z++)for(int x=-3;x<=3;x++)checkPath(w,x,z);
                check(org.nova.novaDungeon.util.RegionUtil.getRegionLevel(map.hub())==0,"Hub is outside combat regions");
                p.setGameMode(GameMode.SURVIVAL);p.getPersistentDataContainer().set(new NamespacedKey(dungeon,"kills"),PersistentDataType.INTEGER,120);p.getPersistentDataContainer().set(new NamespacedKey(dungeon,"boss-round"),PersistentDataType.INTEGER,10);
                double previous=0;
                for(int tier=1;tier<=5;tier++){
                    Location entry=dungeon.entrance(tier);check(entry!=null&&dungeon.safe(entry),"Safe entry tier "+tier);
                    check(org.nova.novaDungeon.util.RegionUtil.getRegionLevel(entry)==tier,"WorldGuard tier boundary "+tier);
                    p.teleport(entry);dungeon.getMobManager().clear();for(int i=0;i<20;i++)dungeon.getMobManager().trySpawn(p,entry,tier);
                    var list=w.getEntitiesByClass(Mob.class).stream().filter(dungeon.getMobManager()::isDungeonMob).toList();check(!list.isEmpty()&&list.size()<=5,"Mob spawn/cap tier "+tier);
                    Mob mob=list.get(0);double hp=mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                    check(mob.getType()==EntityType.valueOf(dungeon.getConfig().getString("tiers."+tier+".mob")),"Correct species tier "+tier);
                    check(hp==dungeon.getConfig().getDouble("tiers."+tier+".health")&&hp>previous,"Increasing health tier "+tier);previous=hp;
                    check(mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).getValue()==dungeon.getConfig().getDouble("tiers."+tier+".damage"),"Increasing attack damage tier "+tier);
                    int priorKills=dungeon.kills(p);mob.damage(10000,p);check(dungeon.kills(p)==priorKills+1,"Death reward progression for species tier "+tier);
                }
                dungeon.getMobManager().clear();p.teleport(dungeon.entrance(1));p.getPersistentDataContainer().set(new NamespacedKey(dungeon,"kills"),PersistentDataType.INTEGER,0);p.setOp(false);
                var move=new org.bukkit.event.player.PlayerMoveEvent(p,p.getLocation(),dungeon.entrance(2));Bukkit.getPluginManager().callEvent(move);check(move.isCancelled(),"Locked zone cannot be entered on foot");
                var teleport=new org.bukkit.event.player.PlayerTeleportEvent(p,p.getLocation(),dungeon.entrance(5));Bukkit.getPluginManager().callEvent(teleport);check(teleport.isCancelled(),"Locked zone cannot be bypassed by teleport");
                var fall=new org.bukkit.event.player.PlayerMoveEvent(p,map.hub(),map.hub().clone().subtract(0,45,0));Bukkit.getPluginManager().callEvent(fall);check(fall.getTo().getY()==81,"Void recovery returns to sanctuary");
                p.setOp(true);p.teleport(map.hub());p.sendMessage("PROBE_MAP_OK");return true;
            }
            if(args.length>0&&args[0].equals("prepare")){
                p.setGameMode(GameMode.SURVIVAL);p.getInventory().clear();p.setHealth(20);p.setFoodLevel(20);
                check(cosmos.hasIsland(p),"Existing Skyblock island detected");
                for(int i=1;i<=3;i++){Location l=dungeon.entrance(i);check(l!=null&&dungeon.safe(l),"Actual dungeon safe entrance tier "+i+": "+l);}
                Location l=p.getLocation().getBlock().getLocation().add(2,0,0);l.getBlock().setType(Material.OBSIDIAN);crates.setCrate("vote",l);
                p.setOp(false);p.performCommand("crate give CosmosProbe vote 64");check(p.getInventory().isEmpty(),"Non-admin cannot issue keys");p.performCommand("dungeonadmin setspawn 1");p.setOp(true);
                check(crates.rewards("vote").size()==NovaCrates.getInstance().getConfig().getStringList("crates.vote.rewards").size(),"Configured rewards validated");
                var original=NovaCrates.getInstance().getConfig().getStringList("crates.vote.rewards");
                NovaCrates.getInstance().getConfig().set("crates.vote.rewards",List.of("ITEM:DIAMOND:2:1"));
                p.getInventory().addItem(crates.createKey("vote"));
                check(crates.open(p,"vote")!=null,"One key opens one crate");
                check(p.getInventory().contains(Material.DIAMOND,2),"Reward delivered synchronously");
                check(crates.open(p,"vote")==null,"Concurrent opening blocked");crates.finish(p);
                check(crates.open(p,"vote")==null,"Spent key cannot be reused");
                p.getInventory().clear();for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Material.STONE,64));p.getInventory().setItem(0,crates.createKey("vote"));
                check(crates.open(p,"vote")==null&&crates.isKey(p.getInventory().getItem(0),"vote"),"Full inventory keeps key");
                p.getInventory().clear();p.getInventory().addItem(crates.createKey("vote"));
                var denied=new org.bukkit.event.player.PlayerInteractEvent(p,org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,null,l.getBlock(),org.bukkit.block.BlockFace.UP,EquipmentSlot.HAND);
                denied.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);Bukkit.getPluginManager().callEvent(denied);
                check(crates.isKey(p.getInventory().getItem(0),"vote"),"Cancelled protection interaction keeps key");
                var offhand=new org.bukkit.event.player.PlayerInteractEvent(p,org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,null,l.getBlock(),org.bukkit.block.BlockFace.UP,EquipmentSlot.OFF_HAND);
                Bukkit.getPluginManager().callEvent(offhand);check(crates.isKey(p.getInventory().getItem(0),"vote"),"Offhand interaction cannot double-open");
                p.getInventory().clear();ItemStack fake=new ItemStack(Material.TRIPWIRE_HOOK);var meta=fake.getItemMeta();meta.setDisplayName(crates.createKey("vote").getItemMeta().getDisplayName());fake.setItemMeta(meta);p.getInventory().addItem(fake);
                check(crates.open(p,"vote")==null,"Renamed fake key rejected");p.getInventory().clear();
                NovaCrates.getInstance().getConfig().set("crates.vote.rewards",List.of("DUST:12:1"));p.getInventory().addItem(crates.createKey("vote"));check(crates.open(p,"vote")!=null,"Crate credits persistent Cosmos dust");crates.finish(p);
                double balance=NovaCrates.econ.getBalance(p);
                NovaCrates.getInstance().getConfig().set("crates.vote.rewards",List.of("MONEY:500:1"));p.getInventory().addItem(crates.createKey("vote"));
                check(crates.open(p,"vote")!=null,"Vault money reward delivered");crates.finish(p);
                check(Math.abs(NovaCrates.econ.getBalance(p)-balance-500)<.01,"Vault balance credited exactly once");
                NovaCrates.getInstance().getConfig().set("crates.vote.rewards",List.of("ITEM:DIAMOND_SWORD:64:1"));
                check(crates.rewards("vote").isEmpty(),"Oversized non-stackable reward rejected");
                NovaCrates.getInstance().getConfig().set("crates.vote.rewards",original);
                p.getPersistentDataContainer().set(new NamespacedKey(dungeon,"kills"),PersistentDataType.INTEGER,0);
                p.setOp(false);check(dungeon.unlocked(p)==1&&!dungeon.eligible(p,2),"Higher tier locked initially");p.setOp(true);
                for(int i=0;i<15;i++)dungeon.recordKill(p);check(dungeon.unlocked(p)==2,"Tier 2 unlocks at 15 kills");
                for(int i=15;i<40;i++)dungeon.recordKill(p);check(dungeon.unlocked(p)==3,"Tier 3 unlocks at 40 kills");
                p.getPersistentDataContainer().set(new NamespacedKey(dungeon,"kills"),PersistentDataType.INTEGER,12);
                p.getPersistentDataContainer().set(new NamespacedKey(dungeon,"boss-round"),PersistentDataType.INTEGER,0);
                p.sendMessage("PROBE_PREPARED");return true;
            }
            if(args.length>0&&args[0].equals("combat")){
                check(dungeon.dungeon(p.getWorld()),"Player entered dungeon through command");
                for(int i=0;i<20;i++)dungeon.getMobManager().trySpawn(p,p.getLocation(),1);
                List<Zombie> enemies=p.getWorld().getEntitiesByClass(Zombie.class).stream().filter(dungeon.getMobManager()::isDungeonMob).toList();
                check(!enemies.isEmpty(),"Safe region-bounded dungeon spawning");
                check(enemies.size()<=5,"Nearby mob cap enforced");
                Zombie boss=enemies.stream().filter(dungeon.getMobManager()::boss).findFirst().orElseThrow();
                int before=dungeon.kills(p);boss.damage(10000,p);
                check(dungeon.kills(p)==before+1,"Actual death event advances progression");
                check(p.getInventory().getContents()!=null&&Arrays.stream(p.getInventory().getContents()).anyMatch(item->crates.isKey(item,"vote")),"Boss death guarantees matching key");
                dungeon.getMobManager().clear();p.sendMessage("PROBE_COMBAT_OK");return true;
            }
            if(args.length>0&&args[0].equals("visual")){
                Location l=crates.locations().get("vote");p.teleport(l.clone().add(-2,1,0));
                Bukkit.getScheduler().runTaskLater(this,()->{try {long n=l.getWorld().getNearbyEntities(l,3,4,3).stream().filter(e->e.getPersistentDataContainer().has(new NamespacedKey("novacrates","crate_holo"),PersistentDataType.STRING)).count();check(n==3,"Crystal, star and hologram exist at configured block");check(l.getBlock().getType()==Material.OBSIDIAN,"Original anchor block preserved");p.sendMessage("PROBE_VISUAL_OK");}catch(Throwable t){p.sendMessage("PROBE_FAIL "+t.getMessage());getLogger().severe(t.toString());}},20);return true;
            }
        }catch(Throwable t){p.setOp(true);p.sendMessage("PROBE_FAIL "+t.getMessage());getLogger().severe(t.toString());}
        return true;
    }
}
