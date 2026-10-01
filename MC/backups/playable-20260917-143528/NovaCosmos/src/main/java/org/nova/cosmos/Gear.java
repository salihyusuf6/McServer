package org.nova.cosmos;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public enum Gear {
    LUNAR_PICK("ay_kazmasi", "§bAy Kazması", Material.IRON_PICKAXE, 9101, 25,
            "§7Sağ tık: 8 saniye acele II", "§8Bekleme: 30 saniye", Enchantment.DIG_SPEED, 2),
    METEOR_PICK("meteor_kazmasi", "§6Meteor Kazması", Material.DIAMOND_PICKAXE, 9102, 160,
            "§7Eğilerek kaz: aynı türden 3×3 kaya", "§8Her blok ada korumasından geçer", Enchantment.DIG_SPEED, 4),
    PULSAR_SWORD("pulsar_kilici", "§dPulsar Kılıcı", Material.IRON_SWORD, 9103, 65,
            "§7Canavara tam güçlü vuruş: +2 hasar", "§8Oyunculara özel hasar uygulanmaz", Enchantment.DAMAGE_ALL, 2),
    NOVA_SWORD("nova_kilici", "§5Süpernova Kılıcı", Material.DIAMOND_SWORD, 9104, 240,
            "§7Canavara tam güçlü vuruş: +4 hasar", "§7Sağ tık: 6 saniye güç I • 35 sn", Enchantment.DAMAGE_ALL, 4),
    ORBIT_HOE("yorunge_capasi", "§aYörünge Çapası", Material.DIAMOND_HOE, 9105, 100,
            "§7Olgun hasat: düşen tohumla yeniden ekim", "§8Buğday, havuç, patates, pancar", Enchantment.DURABILITY, 3),
    VOID_RELIC("bosluk_tilsimi", "§3Boşluk Tılsımı", Material.AMETHYST_SHARD, 9106, 120,
            "§7Sağ tık: 12 saniye yavaş düşüş", "§8Bekleme: 60 saniye", Enchantment.DURABILITY, 1);

    public final String id, title, ability, detail;
    public final Material material;
    public final int model, cost, level;
    private final Enchantment enchant;

    Gear(String id, String title, Material material, int model, int cost, String ability, String detail,
         Enchantment enchant, int level) {
        this.id = id; this.title = title; this.material = material; this.model = model;
        this.cost = cost; this.ability = ability; this.detail = detail; this.enchant = enchant; this.level = level;
    }
    public ItemStack item(NamespacedKey key) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(List.of("§8NOVA COSMOS • UZAY TEKNOLOJİSİ", "", ability, detail, "", "§b✦ " + cost + " Yıldız Tozu"));
        meta.setCustomModelData(model);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        meta.addEnchant(enchant, level, true);
        stack.setItemMeta(meta);
        return stack;
    }
    public static Gear byId(String id) {
        for (Gear gear : values()) if (gear.id.equals(id)) return gear;
        return null;
    }
    public static Gear identify(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) return null;
        Gear gear = byId(stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
        return gear != null && gear.material == stack.getType() ? gear : null;
    }
}
