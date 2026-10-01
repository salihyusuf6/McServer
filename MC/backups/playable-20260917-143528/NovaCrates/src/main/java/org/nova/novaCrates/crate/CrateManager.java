package org.nova.novaCrates.crate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.nova.novaCrates.NovaCrates;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;

public class CrateManager {

    private final NovaCrates plugin;
    private final Map<String, Location> crateLocations = new HashMap<>();
    private final Random random = new Random();

    public CrateManager(NovaCrates plugin) {
        this.plugin = plugin;
        loadLocations();
    }

    public void setCrate(String name, Location loc) {
        crateLocations.put(name, loc);

        if (!plugin.getConfig().isConfigurationSection("crates." + name)) {
            plugin.getConfig().set("crates." + name + ".display", "&a" + name + " Kasası");
            plugin.getConfig().set("crates." + name + ".key-name", "&a" + name + " Anahtarı");
            plugin.getConfig().set("crates." + name + ".hologram", Arrays.asList("&a" + name + " Kasası", "&7Açmak için sağ tıkla!"));
            plugin.getConfig().set("crates." + name + ".rewards", Arrays.asList("ITEM:DIAMOND:1:40", "MONEY:500:60"));
        }

        plugin.getConfig().set("crate-locations." + name,
                loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        plugin.saveConfig();
        
        spawnHologram(name, loc);
    }

    public void removeCrate(String name) {
        crateLocations.remove(name);
        plugin.getConfig().set("crate-locations." + name, null);
        plugin.saveConfig();
    }

    public void spawnHologram(String crateName, Location loc) {
        for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 1.5, 0.5), 1, 2, 1)) {
            if (e.getPersistentDataContainer().has(new NamespacedKey(plugin, "crate_holo"), org.bukkit.persistence.PersistentDataType.STRING)) {
                e.remove();
            }
        }
        List<String> lines = plugin.getConfig().getStringList("crates." + crateName + ".hologram");
        if (lines == null || lines.isEmpty()) return;
        org.bukkit.entity.TextDisplay display = loc.getWorld().spawn(loc.clone().add(0.5, 1.5, 0.5), org.bukkit.entity.TextDisplay.class);
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            if (text.length() > 0) text.append("\n");
            text.append(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
        }
        display.setText(text.toString());
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.getPersistentDataContainer().set(new NamespacedKey(plugin, "crate_holo"), org.bukkit.persistence.PersistentDataType.STRING, crateName);
    }

    public String getCrateAt(Location loc) {
        for (var entry : crateLocations.entrySet()) {
            if (entry.getValue().equals(loc)) return entry.getKey();
        }
        return null;
    }

    private void loadLocations() {
        if (!plugin.getConfig().isConfigurationSection("crate-locations")) return;
        for (String name : plugin.getConfig().getConfigurationSection("crate-locations").getKeys(false)) {
            String[] parts = plugin.getConfig().getString("crate-locations." + name).split(",");
            Location loc = new Location(
                    Bukkit.getWorld(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            );
            crateLocations.put(name, loc);
        }
    }

    public ItemStack createKey(String crateName) {
        String keyName = plugin.getConfig().getString("crates." + crateName + ".key-name");
        if (keyName == null) return null;
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = key.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', keyName));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "crate_type"),
                PersistentDataType.STRING,
                crateName
        );
        key.setItemMeta(meta);
        return key;
    }

    public String getRandomReward(String crate) {
        List<String> rewards = plugin.getConfig().getStringList("crates." + crate + ".rewards");
        int total = 0;
        for (String r : rewards) {
            String[] parts = r.split(":");
            total += Integer.parseInt(parts[parts.length - 1]);
        }
        int rand = random.nextInt(total);
        int current = 0;
        for (String r : rewards) {
            String[] parts = r.split(":");
            current += Integer.parseInt(parts[parts.length - 1]);
            if (rand < current) return r;
        }
        return rewards.get(0);
    }

    public ItemStack parseItem(String reward) {
        String[] parts = reward.split(":");
        if (parts[0].equalsIgnoreCase("ITEM")) {
            ItemStack item = new ItemStack(Material.valueOf(parts[1]), Integer.parseInt(parts[2]));
            return item;
        } else if (parts[0].equalsIgnoreCase("MONEY")) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a" + parts[1] + " Para"));
            item.setItemMeta(meta);
            return item;
        }
        return null;
    }

    public int parseMoney(String reward) {
        String[] parts = reward.split(":");
        if (parts[0].equalsIgnoreCase("MONEY")) {
            return Integer.parseInt(parts[1]);
        }
        return 0;
    }
}