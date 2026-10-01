package org.nova.novaCrates.listeners;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.nova.novaCrates.NovaCrates;
import org.nova.novaCrates.crate.CrateGUI;
import org.nova.novaCrates.crate.CrateManager;

public class CrateListener implements Listener {

    private final CrateManager manager;
    private final CrateGUI gui;

    public CrateListener(CrateManager manager) {
        this.manager = manager;
        this.gui = new CrateGUI(manager);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;

        Location loc = e.getClickedBlock().getLocation();
        String crate = manager.getCrateAt(loc);
        if (crate == null) return;

        Player p = e.getPlayer();
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            gui.preview(p, crate);
            e.setCancelled(true);
            return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!hasKey(p, crate)) {
                p.sendMessage("§cAnahtarın yok!");
                return;
            }

            removeKey(p, crate);
            gui.open(p, crate);
            e.setCancelled(true);
        }
    }

    private boolean hasKey(Player p, String crate) {
        NamespacedKey keyTag = new NamespacedKey(NovaCrates.getInstance(), "crate_type");
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() &&
                    crate.equals(item.getItemMeta().getPersistentDataContainer().get(keyTag, org.bukkit.persistence.PersistentDataType.STRING))) {
                return true;
            }
        }
        return false;
    }

    private void removeKey(Player p, String crate) {
        NamespacedKey keyTag = new NamespacedKey(NovaCrates.getInstance(), "crate_type");
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() &&
                    crate.equals(item.getItemMeta().getPersistentDataContainer().get(keyTag, org.bukkit.persistence.PersistentDataType.STRING))) {
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().contains("Açılıyor") || e.getView().getTitle().contains("Preview")) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().contains("Açılıyor") || e.getView().getTitle().contains("Preview")) e.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        String crate = manager.getCrateAt(loc);
        if (crate != null) {
            if (!e.getPlayer().isOp()) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§cBu kasayı kıramazsın!");
                return;
            }
            for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 1.5, 0.5), 1, 2, 1)) {
                if (ent.getPersistentDataContainer().has(new NamespacedKey(NovaCrates.getInstance(), "crate_holo"), org.bukkit.persistence.PersistentDataType.STRING)) {
                    ent.remove();
                }
            }
            manager.removeCrate(crate);
            e.getPlayer().sendMessage("§aKasa başarıyla silindi!");
        }
    }
}
