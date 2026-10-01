package org.nova.novaDungeon.manager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nova.novaDungeon.NovaDungeon;
import java.nio.file.*;
import java.util.*;

/** Builds only a newly owned world. Existing dungeon/player worlds are never edited. */
public final class DungeonWorld {
    private final NovaDungeon plugin;
    private World world;
    private boolean ready;
    private final List<Entity> labels=new ArrayList<>();
    public static final int FLOOR=80;
    private record BlockEdit(int x,int y,int z,Material material) {}
    public DungeonWorld(NovaDungeon plugin){this.plugin=plugin;}
    public boolean ready(){return ready;}
    public World world(){return world;}
    public static int center(int tier){return 48+(tier-1)*64;}
    public Location hub(){return new Location(world,.5,FLOOR+1,.5,0,0);}
    public Location entrance(int tier){if(!ready||world==null)return null;Location l=new Location(world,.5,FLOOR+1,center(tier)-15+.5,0,0);return plugin.safe(l)?l:null;}
    public void initialize(){
        if(!plugin.getConfig().getBoolean("generated-world",false)){ready=true;return;}
        String name=plugin.getConfig().getString("world","nova_dungeon");
        if(!name.equals("nova_dungeon")){plugin.getLogger().severe("Otomatik harita yalnız nova_dungeon dünyasına kurulabilir.");return;}
        Path marker=plugin.getDataFolder().toPath().resolve("map-v1.ready");
        Path building=plugin.getDataFolder().toPath().resolve("map-v1.building");
        Path folder=Bukkit.getWorldContainer().toPath().resolve(name);
        try {
            if(Files.exists(folder)&&!Files.exists(marker)&&!Files.exists(building))throw new IllegalStateException("nova_dungeon zaten var ve bu eklentiye ait değil; üzerine yazılmadı.");
            if(!Files.exists(marker))Files.writeString(building,"NovaDungeon generated world v1\n");
            world=Bukkit.createWorld(new WorldCreator(name).generator(new ChunkGenerator(){
                @Override public ChunkData generateChunkData(World w,Random r,int x,int z,BiomeGrid biomes){return createChunkData(w);}
            }).generateStructures(false));
            if(world==null)throw new IllegalStateException("Dünya açılamadı");
            world.setDifficulty(Difficulty.NORMAL);world.setTime(18000);world.setStorm(false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.MOB_GRIEFING,false);
            world.setGameRule(GameRule.DO_FIRE_TICK,false);world.setSpawnLocation(hub());
            world.getWorldBorder().setCenter(0,160);world.getWorldBorder().setSize(420);
            if(Files.exists(marker)){regions();ready=true;labels();plugin.getLogger().info("Beş bölgeli zindan dünyası yüklendi.");return;}
            ArrayDeque<BlockEdit> edits=new ArrayDeque<>();
            // Sanctuary, spine walkway and guarded bridges.
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)floor(edits,x,z,Material.SMOOTH_QUARTZ,Material.SEA_LANTERN);
            for(int z=12;z<=center(5)+24;z++)for(int x=-4;x<=4;x++){
                floor(edits,x,z,Math.abs(x)==4?Material.POLISHED_DEEPSLATE:Material.SMOOTH_QUARTZ,Material.SEA_LANTERN);
                if(Math.abs(x)==4)edits.add(new BlockEdit(x,FLOOR+1,z,Material.IRON_BARS));
            }
            Material[] floors={Material.STONE_BRICKS,Material.DEEPSLATE_TILES,Material.POLISHED_BLACKSTONE_BRICKS,Material.BLUE_ICE,Material.POLISHED_DEEPSLATE};
            Material[] trim={Material.PRISMARINE_BRICKS,Material.AMETHYST_BLOCK,Material.RED_NETHER_BRICKS,Material.PACKED_ICE,Material.CRYING_OBSIDIAN};
            for(int tier=1;tier<=5;tier++){
                int cz=center(tier);Material base=floors[tier-1],accent=trim[tier-1];
                for(int x=-24;x<=24;x++)for(int z=cz-24;z<=cz+24;z++){
                    int dz=z-cz;boolean border=Math.abs(x)==24||Math.abs(dz)==24;
                    Material tile=(Math.abs(x)==16||Math.abs(dz)==16)?accent:base;
                    if(Math.abs(x)<=3)tile=Material.SMOOTH_QUARTZ;
                    floor(edits,x,z,tile,Material.SEA_LANTERN);
                    // Clear construction volume only in our newly created world.
                    for(int y=FLOOR+1;y<=FLOOR+16;y++)edits.add(new BlockEdit(x,y,z,Material.AIR));
                    if(border&&!(Math.abs(x)<=4&&Math.abs(dz)==24))for(int y=1;y<=4;y++)edits.add(new BlockEdit(x,FLOOR+y,z,y==4?accent:base));
                }
                for(int x:new int[]{-19,19})for(int dz:new int[]{-19,0,19}){
                    for(int y=1;y<=11;y++)for(int dx=-1;dx<=1;dx++)for(int dz2=-1;dz2<=1;dz2++)edits.add(new BlockEdit(x+dx,FLOOR+y,cz+dz+dz2,y%4==0?accent:base));
                    edits.add(new BlockEdit(x,FLOOR+12,cz+dz,Material.SEA_LANTERN));
                    edits.add(new BlockEdit(x,FLOOR+13,cz+dz,Material.AMETHYST_BLOCK));
                }
                // North/south archways; walking route remains three blocks high and nine wide.
                for(int dz:new int[]{-24,24}){
                    for(int x:new int[]{-5,5})for(int y=1;y<=8;y++)edits.add(new BlockEdit(x,FLOOR+y,cz+dz,accent));
                    for(int x=-5;x<=5;x++)edits.add(new BlockEdit(x,FLOOR+8,cz+dz,accent));
                    edits.add(new BlockEdit(0,FLOOR+9,cz+dz,Material.SEA_LANTERN));
                }
                // Four low obelisks create cover without blocking the center route.
                for(int x:new int[]{-11,11})for(int dz:new int[]{-10,10})for(int y=1;y<=3;y++)edits.add(new BlockEdit(x,FLOOR+y,cz+dz,accent));
            }
            plugin.getLogger().info("Zindan haritası inşa ediliyor: "+edits.size()+" blok işlemi.");
            new BukkitRunnable(){
                public void run(){try{
                    for(int i=0;i<2500&&!edits.isEmpty();i++){BlockEdit e=edits.removeFirst();world.getBlockAt(e.x,e.y,e.z).setType(e.material,false);}
                    if(edits.isEmpty()){regions();world.save();Files.writeString(marker,"NovaDungeon world v1: 5 linked zones\n");Files.deleteIfExists(building);ready=true;labels();plugin.getLogger().info("Zindan haritası hazır: nova_dungeon, 5 bölge, güvenli merkez.");cancel();}
                }catch(Exception e){plugin.getLogger().severe("Harita tamamlanamadı: "+e);cancel();}}
            }.runTaskTimer(plugin,1,1);
        }catch(Exception e){plugin.getLogger().severe("Zindan dünyası hazırlanamadı: "+e);}
    }
    private void floor(Deque<BlockEdit> edits,int x,int z,Material base,Material light){
        edits.add(new BlockEdit(x,FLOOR-1,z,Material.DEEPSLATE));
        edits.add(new BlockEdit(x,FLOOR,z,x%8==0&&z%8==0?light:base));
    }
    private void regions() throws Exception {
        var manager=WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if(manager==null)throw new IllegalStateException("WorldGuard dünya yöneticisi hazır değil");
        for(int i=1;i<=5;i++){
            if(manager.getRegion("level"+i)!=null)continue;
            manager.addRegion(new ProtectedCuboidRegion("level"+i,BlockVector3.at(-24,FLOOR,center(i)-24),BlockVector3.at(24,FLOOR+25,center(i)+24)));
        }
        manager.save();
    }
    private void label(Location l,String text){
        NamespacedKey tag=new NamespacedKey(plugin,"map_label");
        world.getChunkAt(l).load();
        for(Entity e:world.getNearbyEntities(l,2,3,2))if(e.getPersistentDataContainer().has(tag,PersistentDataType.BYTE))e.remove();
        TextDisplay d=world.spawn(l,TextDisplay.class);d.setText(text);d.setBillboard(Display.Billboard.CENTER);d.setBrightness(new Display.Brightness(15,15));d.setShadowed(true);d.setPersistent(true);d.setViewRange(1);d.getPersistentDataContainer().set(tag,PersistentDataType.BYTE,(byte)1);labels.add(d);
    }
    private void labels(){
        label(hub().add(0,3,5),"§b✦ YILDIZ ZİNDANI\n§fGüvenli Merkez\n§7İlerledikçe yaratıklar güçlenir\n§e/zindan cik §7ile geri dön");
        for(int i=1;i<=5;i++)label(new Location(world,.5,FLOOR+5,center(i)-23.5),"§d✦ BÖLGE "+i+" • "+plugin.getConfig().getString("tiers."+i+".name")+"\n§fCan: "+plugin.getConfig().getInt("tiers."+i+".health")+" §8| §cHasar: "+plugin.getConfig().getInt("tiers."+i+".damage")+"\n§7Gerekli av: "+plugin.getConfig().getInt("tiers."+i+".unlock-kills")+" §8• §b"+i+" Yıldız Tozu");
    }
    public void close(){labels.forEach(Entity::remove);labels.clear();}
}
