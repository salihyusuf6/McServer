package org.nova.novaCrates.crate;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.nova.novaCrates.NovaCrates;
import java.util.*;

/** Non-destructive display entities anchored to the configured block. No resource pack needed. */
public final class CrateVisuals {
    private final NovaCrates plugin;private final CrateManager manager;
    private final Map<String,List<Entity>> displays=new HashMap<>();private double phase;
    public CrateVisuals(NovaCrates plugin,CrateManager manager){this.plugin=plugin;this.manager=manager;}
    private NamespacedKey tag(){return new NamespacedKey(plugin,"crate_holo");}
    public void clear(){for(var list:displays.values())for(Entity e:list)e.remove();displays.clear();}
    public void refresh(){clear();manager.loadLocations();tick();}
    private void mark(Entity entity,String name){entity.setPersistent(false);entity.getPersistentDataContainer().set(tag(),PersistentDataType.STRING,name);}
    private void spawn(String name,Location block){
        Location center=block.clone().add(.5,1.5,.5);
        for(Entity old:block.getWorld().getNearbyEntities(center,1,3,1)) if(old.getPersistentDataContainer().has(tag(),PersistentDataType.STRING))old.remove();
        List<Entity> list=new ArrayList<>();
        TextDisplay text=block.getWorld().spawn(block.clone().add(.5,2.7,.5),TextDisplay.class);
        text.setText(manager.display(name)+"\n§fSağ tık §7Aç  §8•  §fSol tık §7Ödüller\n§b✦ Anahtar: Zindan muhafızları");text.setBillboard(Display.Billboard.CENTER);text.setSeeThrough(false);text.setShadowed(true);mark(text,name);list.add(text);
        Material mat=Material.matchMaterial(plugin.getConfig().getString("crates."+name+".visual-material","AMETHYST_BLOCK"));
        if(mat==null||!mat.isBlock()||mat==Material.AIR)mat=Material.AMETHYST_BLOCK;
        BlockDisplay crystal=block.getWorld().spawn(center,BlockDisplay.class);crystal.setBlock(mat.createBlockData());crystal.setBrightness(new Display.Brightness(15,15));crystal.setViewRange(.65f);mark(crystal,name);list.add(crystal);
        ItemDisplay star=block.getWorld().spawn(block.clone().add(.5,2.25,.5),ItemDisplay.class);star.setItemStack(new org.bukkit.inventory.ItemStack(Material.NETHER_STAR));star.setBillboard(Display.Billboard.CENTER);star.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(.45f),new Quaternionf()));star.setBrightness(new Display.Brightness(15,15));mark(star,name);list.add(star);
        displays.put(name,list);
    }
    public void tick(){
        phase+=.12;
        for(var entry:manager.locations().entrySet()){
            Location l=entry.getValue();if(!l.getWorld().isChunkLoaded(l.getBlockX()>>4,l.getBlockZ()>>4))continue;
            boolean nearby=l.getWorld().getPlayers().stream().anyMatch(p->p.getLocation().distanceSquared(l)<32*32);
            if(!nearby)continue;
            var list=displays.get(entry.getKey());if(list==null||list.stream().anyMatch(e->!e.isValid())){if(list!=null)list.forEach(Entity::remove);spawn(entry.getKey(),l);list=displays.get(entry.getKey());}
            BlockDisplay crystal=(BlockDisplay)list.get(1);
            crystal.setInterpolationDelay(0);crystal.setInterpolationDuration(5);
            crystal.setTransformation(new Transformation(new Vector3f(0,(float)Math.sin(phase)*.08f,0),new Quaternionf().rotateY((float)phase).rotateZ(.785f).rotateX(.615f),new Vector3f(.52f),new Quaternionf()));
            Color color=entry.getKey().equals("vote")?Color.AQUA:entry.getKey().equals("tarım")?Color.LIME:Color.FUCHSIA;
            for(int i=0;i<12;i++){double a=phase+i*Math.PI/6;Location pos=l.clone().add(.5+Math.cos(a)*.7,1.45,.5+Math.sin(a)*.7);l.getWorld().spawnParticle(Particle.REDSTONE,pos,1,0,0,0,0,new Particle.DustOptions(color,.8f));}
        }
    }
}
