package com.Bannan.bannanDefaultLobby;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class MyEventListener implements Listener {
    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent e){
        Player p = e.getPlayer();
        String name = p.getName();
        if (BannanDefaultLobby.DefaultSpawnLocation == null){
            BannanDefaultLobby.DefaultSpawnLocation = new Location(Bukkit.getWorld(BannanDefaultLobby.SpawnWorldName),BannanDefaultLobby.DefaultSpawnPosition.getX(),BannanDefaultLobby.DefaultSpawnPosition.getY(),BannanDefaultLobby.DefaultSpawnPosition.getZ());
        }
        Bukkit.getScheduler().runTaskAsynchronously(BannanDefaultLobby.MyPlugin,()->{
           //Async Part
            String msg;
            try{
                msg = Network.PlayerCheckEvent(name,String.format("%s/NameCheck",BannanDefaultLobby.BackendServerURL));
            }
            catch (Exception exception){
                msg = exception.toString();
            }

            String finalMsg = msg;//Random Bullshit
            Bukkit.getScheduler().runTask(BannanDefaultLobby.MyPlugin,()->{
                if (!finalMsg.toLowerCase().equals("True".toLowerCase())){
                    p.kick(Component.text("Player can't found.\nYou can join our familiy any time on mc.trybanna.com \n"));
                }
                p.setGameMode(GameMode.SURVIVAL);
                p.teleport(BannanDefaultLobby.DefaultSpawnLocation);
                p.getPersistentDataContainer().set(BannanDefaultLobby.LoginCheck, PersistentDataType.BOOLEAN,false);
           }) ;
        });
    }
    @EventHandler
    public void DisableDamages(EntityDamageEvent e){
        e.setCancelled(true);
        if(e.getEntity() instanceof Player){
            Player p = (Player)e.getEntity();
            if (p.getLocation().getBlockY() < -10){
                p.teleport(BannanDefaultLobby.DefaultSpawnLocation);
            }
        }
    }
    @EventHandler
    public void WhitstandMovement(PlayerMoveEvent e){
        Player p = e.getPlayer();
        PersistentDataContainer con = p.getPersistentDataContainer();
        Boolean temp = con.get(BannanDefaultLobby.LoginCheck,PersistentDataType.BOOLEAN);
        if (temp != null){
            e.setCancelled(!temp);
        }
    }
    @EventHandler
    public void CatchSkyblockSoilStep(PlayerMoveEvent e){
        if (e.getTo() == null || e.getFrom().getBlock().equals(e.getTo().getBlock())){
            return;
        }
        Player p = e.getPlayer();
        PersistentDataContainer con = p.getPersistentDataContainer();
        Boolean loggedIn = con.get(BannanDefaultLobby.LoginCheck, PersistentDataType.BOOLEAN);
        if (loggedIn == null || !loggedIn){
            return;
        }
        Material standingOn = e.getTo().clone().subtract(0,1,0).getBlock().getType();
        if (standingOn == Material.DIRT || standingOn == Material.COARSE_DIRT || standingOn == Material.ROOTED_DIRT || standingOn == Material.FARMLAND){
            Network.SendPlayerToServer(p,"skyblock");
        }
    }
    @EventHandler
    public void WhitstandCommands(PlayerCommandPreprocessEvent e){
        Player p = e.getPlayer();
        PersistentDataContainer con = p.getPersistentDataContainer();
        Boolean temp = con.get(BannanDefaultLobby.LoginCheck,PersistentDataType.BOOLEAN);
        String raw = e.getMessage();
        String temp_ = raw.split(" ")[0];
        if ((!temp_.toLowerCase().equals("/login"))&&(!temp_.toLowerCase().equals("/l"))) {
            e.setCancelled(!temp);
        }
    }
    @EventHandler
    public void WhitstandMessages(AsyncChatEvent e){
        Player p = e.getPlayer();
        PersistentDataContainer con = p.getPersistentDataContainer();
        Boolean temp = con.get(BannanDefaultLobby.LoginCheck,PersistentDataType.BOOLEAN);
        e.setCancelled(!temp);
    }
    @EventHandler
    public void DisableAnimalBreeding(EntityBreedEvent e) {
        e.setCancelled(true);
    }
    @EventHandler
    public void WhitstandBlockBreaking(BlockBreakEvent e)
    {
        e.setCancelled(true);
    }
    @EventHandler
    public void WhitstandBlockPlacement(BlockPlaceEvent e){
        e.setCancelled(true);
    }
    @EventHandler
    public void WhitstandInventoryChanging(InventoryClickEvent e){
        e.setCancelled(true);
    }
    @EventHandler
    public void WhitstandItemDropEvent(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }
    @EventHandler
    public void WhitstamdPlayerHandSwap(PlayerSwapHandItemsEvent e) {
        e.setCancelled(true);
    }
    @EventHandler
    public void CatchGameNpcInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Villager)){
            return;
        }
        Villager villager = (Villager) e.getRightClicked();
        Boolean isGameNpc = villager.getPersistentDataContainer().get(BannanDefaultLobby.NpcTagKey, PersistentDataType.BOOLEAN);
        if (isGameNpc == null || !isGameNpc){
            return;
        }
        e.setCancelled(true);
        e.getPlayer().openInventory(UIManager.GameModeSelecterUI());
    }
    @EventHandler
    public void CatchGameModeSelecterUIClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof UIManager.GameModeSelecterHolder)){
            return;
        }
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(top)){
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()){
            return;
        }
        Boolean isSkyblockButton = clicked.getItemMeta().getPersistentDataContainer().get(BannanDefaultLobby.SkyblockButtonKey, PersistentDataType.BOOLEAN);
        if (isSkyblockButton == null || !isSkyblockButton){
            return;
        }
        Player p = (Player) e.getWhoClicked();
        p.closeInventory();
        Network.SendPlayerToServer(p,"skyblock");
    }
    @EventHandler
    public void CatchPlayerRightclikEvent(PlayerInteractEvent e) {
        if(e.getAction().isRightClick()){
            e.setCancelled(true);
            Player p = e.getPlayer();
            p.sendMessage(p.getInventory().getItemInMainHand().toString());
            if (p.getInventory().getItemInMainHand().getType() == Material.COMPASS){
             p.openInventory(UIManager.GameModeSelecterUI());
            }
        }
    }
}
