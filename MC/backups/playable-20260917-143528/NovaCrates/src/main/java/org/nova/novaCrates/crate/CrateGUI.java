package org.nova.novaCrates.crate;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.nova.novaCrates.NovaCrates;

public class CrateGUI {

    private final CrateManager manager;

    public CrateGUI(CrateManager manager) {
        this.manager = manager;
    }

    public void open(Player p, String crate) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Açılıyor...");
        
        ItemStack pointer = new ItemStack(org.bukkit.Material.RED_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta pm = pointer.getItemMeta();
        pm.setDisplayName("§c↓ Ödül ↓");
        pointer.setItemMeta(pm);
        inv.setItem(4, pointer);
        
        ItemStack pointerUp = new ItemStack(org.bukkit.Material.RED_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta pum = pointerUp.getItemMeta();
        pum.setDisplayName("§c↑ Ödül ↑");
        pointerUp.setItemMeta(pum);
        inv.setItem(22, pointerUp);

        p.openInventory(inv);

        String winningReward = manager.getRandomReward(crate);

        new BukkitRunnable() {
            int ticks = 0;
            int stopTick = 40;

            @Override
            public void run() {
                if (ticks >= stopTick) {
                    giveReward(p, winningReward);
                    cancel();
                    
                    Bukkit.getScheduler().runTaskLater(NovaCrates.getInstance(), () -> {
                        if (p.getOpenInventory().getTopInventory().equals(inv)) {
                            p.closeInventory();
                        }
                    }, 40L); // 2 saniye sonra kapat
                    return;
                }

                for (int i = 9; i < 17; i++) inv.setItem(i, inv.getItem(i + 1));

                if (ticks == stopTick - 5) {
                    inv.setItem(17, manager.parseItem(winningReward));
                } else {
                    String randomFill = manager.getRandomReward(crate);
                    inv.setItem(17, manager.parseItem(randomFill));
                }
                
                ticks++;
            }
        }.runTaskTimer(NovaCrates.getInstance(), 0, 2);
    }

    public void preview(Player p, String crate) {
        Inventory inv = Bukkit.createInventory(null, 27, "§ePreview");
        var rewards = NovaCrates.getInstance().getConfig().getStringList("crates." + crate + ".rewards");
        int slot = 0;
        for (String r : rewards) inv.setItem(slot++, manager.parseItem(r));
        p.openInventory(inv);
    }

    private void giveReward(Player p, String reward) {
        String[] parts = reward.split(":");
        if (parts[0].equalsIgnoreCase("ITEM")) {
            ItemStack item = manager.parseItem(reward);
            if (item != null) p.getInventory().addItem(item);
        } else if (parts[0].equalsIgnoreCase("MONEY")) {
            int money = manager.parseMoney(reward);
            if (money > 0 && NovaCrates.econ != null) {
                NovaCrates.econ.depositPlayer(p, money);
            }
        }

        int chance = Integer.parseInt(parts[parts.length - 1]);
        if (chance < 30) {
            String prizeName = parts[0].equalsIgnoreCase("ITEM") ? parts[2] + "x " + parts[1] : parts[1] + " Para";
            Bukkit.broadcastMessage("§8[§6Kasa§8] §a" + p.getName() + " §7adlı oyuncu kasadan §e" + prizeName + " §7çıkardı!");
        }
    }
}