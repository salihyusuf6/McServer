package org.nova.novaCrates.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.*;
import org.bukkit.NamespacedKey;
import org.nova.novaCrates.NovaCrates;

public class KeyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length < 1) return true;

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) return true;

        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = key.getItemMeta();

        NamespacedKey keyTag = new NamespacedKey(NovaCrates.getInstance(), "crate_key");

        meta.setDisplayName("§6Nova Kasa Anahtarı");
        meta.getPersistentDataContainer().set(keyTag, PersistentDataType.STRING, "nova_key");

        key.setItemMeta(meta);

        target.getInventory().addItem(key);
        target.sendMessage("§aKasa anahtarı aldın!");

        return true;
    }
}