package org.nova.novaDungeon.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class AdminGUI {

    public static void open(Player p) {

        Inventory inv = Bukkit.createInventory(null, 27, "Dungeon Admin");

        inv.setItem(11, new org.bukkit.inventory.ItemStack(Material.ZOMBIE_HEAD));
        inv.setItem(13, new org.bukkit.inventory.ItemStack(Material.NETHER_STAR));
        inv.setItem(15, new org.bukkit.inventory.ItemStack(Material.DIAMOND));

        p.openInventory(inv);
    }
}