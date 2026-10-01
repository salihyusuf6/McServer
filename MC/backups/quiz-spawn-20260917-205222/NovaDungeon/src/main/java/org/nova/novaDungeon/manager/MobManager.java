package org.nova.novaDungeon.manager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.nova.novaDungeon.NovaDungeon;
import org.nova.novaDungeon.util.RegionUtil;
import java.util.*;

public class MobManager {
    private final Map<UUID,Long> mobs=new HashMap<>();private final Random random=new Random();
    private NovaDungeon plugin(){return NovaDungeon.getInstance();}
    private NamespacedKey key(String name){return new NamespacedKey(plugin(),name);}
    public int level(Entity e){return e.getPersistentDataContainer().getOrDefault(key("level"),PersistentDataType.INTEGER,0);}
    public boolean boss(Entity e){return e.getPersistentDataContainer().has(key("boss"),PersistentDataType.BYTE);}
    public boolean isDungeonMob(Entity e){return level(e)>0;}
    public void remove(Entity e){mobs.remove(e.getUniqueId());}
    public void clear(){for(UUID id:mobs.keySet()){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}mobs.clear();}
    public void tick(){
        long now=System.currentTimeMillis();
        mobs.entrySet().removeIf(entry->{Entity e=Bukkit.getEntity(entry.getKey());if(e==null||!e.isValid())return true;if(entry.getValue()<now){e.remove();return true;}return false;});
        if(plugin().generatedWorld()!=null&&!plugin().generatedWorld().ready())return;
        World world=Bukkit.getWorld(plugin().getConfig().getString("world","dungeon"));if(world==null)return;
        for(Player p:world.getPlayers()){
            int level=RegionUtil.getRegionLevel(p.getLocation());
            if(level>0 && plugin().eligible(p,level))trySpawn(p,p.getLocation(),level);
        }
        for(UUID id:List.copyOf(mobs.keySet())){
            Entity entity=Bukkit.getEntity(id);if(!(entity instanceof Mob z))continue;
            if(RegionUtil.getRegionLevel(z.getLocation())!=level(z)){z.remove();mobs.remove(id);continue;}
            if(boss(z) && random.nextInt(4)==0 && z.getTarget() instanceof Player target){
                Location warning=target.getLocation().clone();int tier=level(z);
                warning.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,warning.clone().add(0,.2,0),24,.8,.05,.8,.01);
                target.sendMessage("§5✦ Muhafız darbesi! §eIşıklı alandan uzaklaş!");
                warning.getWorld().playSound(warning,Sound.ENTITY_EVOKER_PREPARE_ATTACK,.7f,1.2f);
                Bukkit.getScheduler().runTaskLater(plugin(),()->{
                    if(!z.isValid()||z.isDead())return;
                    warning.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,warning,1);
                    for(Player p:warning.getWorld().getPlayers())if(p.getLocation().distanceSquared(warning)<4 && plugin().eligible(p,tier))p.damage(3+tier,z);
                },25);
            }
        }
    }
    public void trySpawn(Player player,Location location,int level){
        if(mobs.size()>=plugin().getConfig().getInt("spawning.global-cap",36))return;
        long nearby=mobs.keySet().stream().map(Bukkit::getEntity).filter(Objects::nonNull).filter(e->e.getWorld().equals(location.getWorld())&&e.getLocation().distanceSquared(location)<18*18).count();
        if(nearby>=plugin().getConfig().getInt("spawning.nearby-cap",5))return;
        boolean boss=plugin().kills(player)>0 && plugin().kills(player)/12>player.getPersistentDataContainer().getOrDefault(key("boss-round"),PersistentDataType.INTEGER,0);
        if(boss && mobs.keySet().stream().map(Bukkit::getEntity).filter(Objects::nonNull).anyMatch(e->boss(e)&&level(e)==level))boss=false;
        for(int attempt=0;attempt<16;attempt++){
            Location spawn=location.clone().add(random.nextInt(13)-6,0,random.nextInt(13)-6);spawn.setX(spawn.getBlockX()+.5);spawn.setZ(spawn.getBlockZ()+.5);
            if(spawn.distanceSquared(location)<16||RegionUtil.getRegionLevel(spawn)!=level||!plugin().safe(spawn))continue;
            EntityType type=EntityType.valueOf(plugin().getConfig().getString("tiers."+level+".mob","ZOMBIE"));
            if(!Set.of(EntityType.ZOMBIE,EntityType.HUSK,EntityType.SKELETON,EntityType.STRAY,EntityType.WITHER_SKELETON).contains(type))return;
            Mob z=(Mob)location.getWorld().spawnEntity(spawn,type);
            if(z instanceof Zombie zombie){zombie.setAdult();zombie.setShouldBurnInDay(false);}
            z.setCanPickupItems(false);z.setRemoveWhenFarAway(false);z.setPersistent(false);
            z.getPersistentDataContainer().set(key("level"),PersistentDataType.INTEGER,level);
            if(boss){z.getPersistentDataContainer().set(key("boss"),PersistentDataType.BYTE,(byte)1);player.getPersistentDataContainer().set(key("boss-round"),PersistentDataType.INTEGER,plugin().kills(player)/12);player.saveData();}
            z.setCustomName((boss?"§5✦ Yıldız Muhafızı":"§b✧ "+plugin().getConfig().getString("tiers."+level+".mob-name","Kozmik Gezgin"))+" §7[Seviye "+level+"]");z.setCustomNameVisible(true);
            double hp=plugin().getConfig().getDouble("tiers."+level+".health",20*level)*(boss?3:1);
            z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1,hp));z.setHealth(Math.max(1,hp));
            z.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(plugin().getConfig().getDouble("tiers."+level+".damage",2+level)*(boss?1.4:1));
            z.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(plugin().getConfig().getDouble("tiers."+level+".speed",.21));
            z.getEquipment().clear();
            if(z instanceof AbstractSkeleton)z.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(type==EntityType.WITHER_SKELETON?Material.IRON_SWORD:Material.BOW));
            z.getEquipment().setItemInMainHandDropChance(0);z.getEquipment().setItemInOffHandDropChance(0);
            z.getEquipment().setChestplateDropChance(0);z.getEquipment().setLeggingsDropChance(0);z.getEquipment().setBootsDropChance(0);
            z.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(boss?Material.AMETHYST_BLOCK:Material.CHAINMAIL_HELMET));z.getEquipment().setHelmetDropChance(0);
            z.setTarget(player);mobs.put(z.getUniqueId(),System.currentTimeMillis()+180000);return;
        }
    }
}
