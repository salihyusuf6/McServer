package org.nova.novaDungeon.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nova.novaDungeon.NovaDungeon;

import java.util.Random;

public class DeathListener implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onDeath(EntityDeathEvent e) {

        if (!(e.getEntity() instanceof Zombie)) return;

        Zombie z = (Zombie) e.getEntity();

        if (!NovaDungeon.getInstance().getMobManager().isDungeonMob(z)) return;

        Player p = z.getKiller();
        if (p == null) return;

        e.getDrops().clear();

        ItemStack item;

        if (random.nextInt(100) < 10) {
            item = rare();
        } else {
            item = normal(z.getCustomName());
        }

        p.getInventory().addItem(item);

        NovaDungeon.getInstance().getMobManager().remove(z);
    }

    private ItemStack normal(String name) {
        ItemStack i = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§6Loot " + name);
        i.setItemMeta(m);
        return i;
    }

    private ItemStack rare() {
        ItemStack i = new ItemStack(Material.DIAMOND);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§bRARE ITEM ✨");
        i.setItemMeta(m);
        return i;
    }
}