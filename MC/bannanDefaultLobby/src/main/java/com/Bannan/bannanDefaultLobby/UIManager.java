package com.Bannan.bannanDefaultLobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class UIManager {
    public static final int SKYBLOCK_BUTTON_SLOT = 22;

    public static class GameModeSelecterHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    public static Inventory GameModeSelecterUI(){
        GameModeSelecterHolder holder = new GameModeSelecterHolder();
        Inventory temp = Bukkit.createInventory(holder,54,"GameModeSelecter");
        holder.setInventory(temp);

        ItemStack skyblockButton = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = skyblockButton.getItemMeta();
        meta.displayName(Component.text("Skyblock", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC,false));
        meta.lore(java.util.List.of(Component.text("Skyblock sunucusuna ışınlanmak için tıkla", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false)));
        meta.getPersistentDataContainer().set(BannanDefaultLobby.SkyblockButtonKey, PersistentDataType.BOOLEAN, true);
        skyblockButton.setItemMeta(meta);
        temp.setItem(SKYBLOCK_BUTTON_SLOT, skyblockButton);

        return temp;
    }
}
