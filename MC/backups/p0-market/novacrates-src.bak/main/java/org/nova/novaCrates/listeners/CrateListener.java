package org.nova.novaCrates.listeners;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.nova.novaCrates.crate.*;
import org.nova.novaCrates.NovaCrates;
public class CrateListener implements Listener {
    private final CrateManager manager;private final CrateGUI gui;
    public CrateListener(CrateManager m){manager=m;gui=new CrateGUI(m);}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){
        if(e.getClickedBlock()==null || e.useInteractedBlock()==Event.Result.DENY)return;String name=manager.getCrateAt(e.getClickedBlock().getLocation());if(name==null)return;
        e.setCancelled(true);if(e.getHand()!=EquipmentSlot.HAND)return;
        if(e.getAction()==Action.LEFT_CLICK_BLOCK)gui.preview(e.getPlayer(),name);
        if(e.getAction()==Action.RIGHT_CLICK_BLOCK)gui.open(e.getPlayer(),name);
    }
    @EventHandler public void click(InventoryClickEvent e){if(e.getView().getTopInventory().getHolder() instanceof CrateGUI.Holder)e.setCancelled(true);}
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof CrateGUI.Holder)e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){manager.finish(e.getPlayer());}
    @EventHandler public void load(WorldLoadEvent e){NovaCrates.getInstance().visuals().refresh();}
    @EventHandler public void breaking(BlockBreakEvent e){if(manager.getCrateAt(e.getBlock().getLocation())!=null){e.setCancelled(true);e.getPlayer().sendMessage("§cBu blok bir kasa. Yönetici: /crate remove <kasa>");}}
    @EventHandler public void explode(EntityExplodeEvent e){e.blockList().removeIf(b->manager.getCrateAt(b.getLocation())!=null);}
    @EventHandler public void explode(BlockExplodeEvent e){e.blockList().removeIf(b->manager.getCrateAt(b.getLocation())!=null);}
    @EventHandler public void extend(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(b->manager.getCrateAt(b.getLocation())!=null||manager.getCrateAt(b.getRelative(e.getDirection()).getLocation())!=null))e.setCancelled(true);}
    @EventHandler public void retract(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(b->manager.getCrateAt(b.getLocation())!=null))e.setCancelled(true);}
}
