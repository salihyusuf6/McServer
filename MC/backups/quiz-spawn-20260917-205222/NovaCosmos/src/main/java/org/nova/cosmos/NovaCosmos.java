package org.nova.cosmos;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.events.IslandGenerateBlockEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.boss.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class NovaCosmos extends JavaPlugin implements Listener, TabExecutor {
    private NamespacedKey itemKey, naturalKey, deliveryKey;
    private ProgressStore store;
    private final Map<Location, Material> generated = new HashMap<>();
    private final Set<UUID> excavating = new HashSet<>();
    private final Map<UUID, Map<Gear, Long>> cooldowns = new HashMap<>();
    private final String[] labels = {"Meteor Madenciliği", "Kozmik İstila", "Yıldız Hasadı"};
    private final String[] types = {"mining", "combat", "farming"};
    private int eventType = -1, rotation;
    private String eventId = "";
    private long eventEnd, nextEvent;
    private BossBar bar;
    private PackHost packHost;
    private String packUrl = "", packHash = "";
    private final UUID packId = UUID.fromString("43787de9-a09b-439b-a455-418f81020318");
    private double starPhase;

    @Override public void onEnable() {
        saveDefaultConfig();
        itemKey = new NamespacedKey(this, "gear"); naturalKey = new NamespacedKey(this, "natural"); deliveryKey = new NamespacedKey(this, "delivery");
        try { store = new ProgressStore(getDataFolder().toPath().resolve("players")); }
        catch (Exception e) { getLogger().log(Level.SEVERE, "Oyuncu verileri açılamadı", e); getServer().getPluginManager().disablePlugin(this); return; }
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("uzay")).setExecutor(this);
        getCommand("uzay").setTabCompleter(this);
        nextEvent = System.currentTimeMillis() + interval() * 1000L;
        restoreEvent();
        setupPack();
        getServer().getScheduler().runTaskTimer(this, this::tick, 20, 20);
        getServer().getScheduler().runTaskTimer(this, this::orbitStars, 5, 5);
        getServer().getScheduler().runTaskTimer(this, () -> {
            try { store.flush(); } catch (Exception e) { storageError(e); }
        }, 1200, 1200);
        getLogger().info("Nova Cosmos hazır: 6 ekipman, 3 günlük görev, 3 dönüşümlü etkinlik.");
    }
    @Override public void onDisable() {
        if (packHost != null) packHost.close();
        if (bar != null) bar.removeAll();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) p.closeInventory();
        if (store != null) try { store.flush(); } catch (Exception e) { storageError(e); }
    }
    private int positive(String path, int fallback) { return Math.max(1, Math.min(1_000_000, getConfig().getInt(path, fallback))); }
    private int interval() { return positive("events.interval-seconds", 5400); }
    private int duration() { return positive("events.duration-seconds", 900); }
    private int target(int type, boolean event) { return positive((event ? "events." : "daily.") + types[type] + "-target", 100); }
    private void storageError(Exception e) { getLogger().log(Level.SEVERE, "Cosmos verisi kaydedilemedi; işlem durduruldu", e); }
    private void tell(CommandSender p, String text) { p.sendMessage("§8[§b✦ Cosmos§8] §7" + text); }
    private Progress data(Player p) throws Exception {
        Progress d = store.get(p.getUniqueId()); d.rollover(Progress.today()); return d;
    }
    private boolean transaction(Player p, Consumer<Progress> change) {
        try {
            Progress copy = data(p).copy(); change.accept(copy); store.save(p.getUniqueId(), copy); return true;
        } catch (Exception e) { storageError(e); tell(p, "§cKayıt yapılamadı. İşlem uygulanmadı; yöneticiye bildir."); return false; }
    }
    /** Main-thread integration API. Persist currency before reporting success. */
    public boolean creditDust(Player player, int amount) {
        if (!Bukkit.isPrimaryThread() || amount <= 0 || amount > 100000) return false;
        return transaction(player, d -> d.dust = Math.addExact(d.dust, amount));
    }
    public boolean hasIsland(Player player) {
        return SuperiorSkyblockAPI.getPlayer(player).getIsland() != null;
    }
    private boolean survival(Player p) { return p.getGameMode() == GameMode.SURVIVAL; }
    private boolean island(Player p, Location loc) {
        var i = SuperiorSkyblockAPI.getIslandAt(loc);
        return i != null && i.isInsideRange(loc) && i.isMember(SuperiorSkyblockAPI.getPlayer(p));
    }
    private boolean combatArea(Player p, Location loc) {
        return island(p, loc) || getConfig().getStringList("combat-worlds").contains(loc.getWorld().getName());
    }
    private void progress(Player p, int type) {
        if (!survival(p)) return;
        try {
            Progress d = data(p);
            if (d.counts[type] < target(type, false)) {
                d.counts[type]++;
                if (d.counts[type] == target(type, false)) tell(p, "§a" + labels[type] + " görevi tamamlandı! §f/uzay gorev");
            }
            if (eventType == type && System.currentTimeMillis() < eventEnd) {
                d.event(eventId);
                if (d.eventCount < target(type, true)) {
                    d.eventCount++;
                    if (d.eventCount == target(type, true)) tell(p, "§dEtkinlik hedefi tamamlandı! Ödül: §f/uzay odul");
                }
            }
        } catch (Exception e) { storageError(e); }
    }
    private static final class Menu implements InventoryHolder {
        final UUID owner; Inventory inventory;
        Menu(UUID owner) { this.owner = owner; }
        public Inventory getInventory() { return inventory; }
    }
    private ItemStack icon(Material material, String title, String... lore) {
        ItemStack i = new ItemStack(material); var m = i.getItemMeta();
        m.setDisplayName(title); m.setLore(Arrays.asList(lore)); i.setItemMeta(m); return i;
    }
    private void menu(Player p) {
        try {
            Progress d = data(p);
            Menu holder = new Menu(p.getUniqueId());
            Inventory inv = Bukkit.createInventory(holder, 54, "§0✦ NOVA COSMOS • Kontrol Merkezi"); holder.inventory = inv;
            for (int i = 0; i < 54; i++) inv.setItem(i, icon(Material.BLACK_STAINED_GLASS_PANE, " "));
            inv.setItem(4, icon(Material.NETHER_STAR, "§b✦ " + d.dust + " Yıldız Tozu", "§7Görev, etkinlik ve zindanlarda kazan.", "§7Ekipmana tıkla → bedeliyle üret.", "§8Satın almadan önce çantanda yer aç."));
            int[] slots = {19, 20, 21, 23, 24, 25};
            for (int i = 0; i < slots.length; i++) inv.setItem(slots[i], Gear.values()[i].item(itemKey));
            inv.setItem(10, icon(Material.CHEST, "§eBaşlangıç Hibesi", d.started ? "§8Zaten alındı" : "§7Bir kez 25 Yıldız Tozu → Ay Kazması"));
            inv.setItem(13, icon(Material.END_PORTAL_FRAME, "§d" + (eventType < 0 ? "Sonraki kozmik olay" : labels[eventType]),
                    eventType < 0 ? "§7Başlamasına: " + Math.max(0, (nextEvent - System.currentTimeMillis()) / 60000) + " dk" : "§7Kalan: " + Math.max(0, (eventEnd - System.currentTimeMillis()) / 1000) + " sn",
                    "§7Ödülü almak için tıkla.", "§8Hedef ve ilerleme: /uzay etkinlik"));
            for (int i = 0; i < 3; i++) inv.setItem(38 + i * 2, icon(Material.BOOK, "§a" + labels[i],
                    "§7" + d.counts[i] + " / " + target(i, false), "§bÖdül: " + positive("daily.reward", 35) + " Yıldız Tozu",
                    d.claimed[i] ? "§8Ödül alındı" : "§eTamamlandıysa tıkla ve al."));
            inv.setItem(46, icon(Material.IRON_SWORD, "§dYıldız Zindanı", "§7Üç bölge • muhafızlar • yıldız tozu", "§eGiriş ve rehber için tıkla."));
            inv.setItem(52, icon(Material.ENDER_CHEST, "§bNova Kasaları", "§7Zindandan anahtar kazan.", "§eKasaları görmek için tıkla."));
            inv.setItem(49, icon(Material.COMPASS, "§fUçuş El Kitabı", "§7Ada oluştur: /is", "§7Jeneratör kaz • doğal canavar kes • hasat yap",
                    "§7Görevler İstanbul saatiyle gece yarısı yenilenir.", "§7Görünümler: /uzay paket", "§8Paket olmadan da özellikler çalışır."));
            p.openInventory(inv);
        } catch (Exception e) { storageError(e); tell(p, "§cVerilerin okunamadı; yöneticiye bildir."); }
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu m)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || !m.owner.equals(p.getUniqueId()) || !e.isLeftClick() || e.isShiftClick()) return;
        int slot = e.getRawSlot(); int[] slots = {19, 20, 21, 23, 24, 25};
        for (int i = 0; i < slots.length; i++) if (slot == slots[i]) { buy(p, Gear.values()[i]); menu(p); return; }
        if (slot == 46 || slot == 52) {
            Bukkit.getScheduler().runTask(this, () -> { p.closeInventory(); p.performCommand(slot == 46 ? "zindan" : "crate"); });
            return;
        }
        if (slot == 10) starter(p);
        if (slot == 13) claimEvent(p);
        for (int i = 0; i < 3; i++) if (slot == 38 + i * 2) claim(p, i);
        if (slot == 10 || slot == 13 || slot == 38 || slot == 40 || slot == 42) menu(p);
    }
    @EventHandler public void drag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }
    private void starter(Player p) {
        try {
            if (data(p).started) { tell(p, "Başlangıç hibeni zaten aldın."); return; }
            if (transaction(p, d -> { d.started = true; d.dust += 25; })) tell(p, "§a25 Yıldız Tozu kazandın! /uzay → Ay Kazması üret.");
        } catch (Exception e) { storageError(e); }
    }
    private void buy(Player p, Gear gear) {
        try {
            if (!deliver(p)) return;
            int slot = p.getInventory().firstEmpty();
            if (slot < 0) { tell(p, "§cÇantanda boş yer aç."); return; }
            if (data(p).dust < gear.cost) { tell(p, "§cYetersiz Yıldız Tozu: " + gear.cost + " gerekli."); return; }
            // Persist debit first; normal click/shift/number-key duplication cannot mint equipment.
            if (transaction(p, d -> {
                if (!d.debit(gear.cost)) throw new IllegalStateException("Bakiye değişti");
                d.pendingGear = gear.id; d.deliveryId = UUID.randomUUID().toString();
            }) && deliver(p)) {
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, .6f, 1.6f);
                tell(p, gear.title + " §aüretildi!");
            }
        } catch (Exception e) { storageError(e); }
    }
    private boolean deliver(Player p) throws Exception {
        Progress d = data(p);
        if (d.pendingGear.isEmpty()) return true;
        Gear gear = Gear.byId(d.pendingGear);
        if (gear == null || d.deliveryId.isEmpty()) throw new IllegalStateException("Geçersiz bekleyen teslimat");
        boolean exists = false;
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && i.hasItemMeta() && d.deliveryId.equals(i.getItemMeta().getPersistentDataContainer().get(deliveryKey, PersistentDataType.STRING))) { exists = true; break; }
        }
        if (!exists) {
            int slot = p.getInventory().firstEmpty();
            if (slot < 0) { tell(p, "§eBekleyen ekipmanın var. Çantanda yer açıp /uzay teslim yaz."); return false; }
            ItemStack i = gear.item(itemKey); var meta = i.getItemMeta();
            meta.getPersistentDataContainer().set(deliveryKey, PersistentDataType.STRING, d.deliveryId); i.setItemMeta(meta);
            p.getInventory().setItem(slot, i); p.saveData();
        }
        return transaction(p, v -> { v.pendingGear = ""; v.deliveryId = ""; });
    }
    private void claim(Player p, int type) {
        try {
            Progress d = data(p);
            if (d.claimed[type] || d.counts[type] < target(type, false)) { tell(p, "Bu görev henüz tamamlanmadı veya ödülü alındı."); return; }
            if (transaction(p, v -> v.claim(type, target(type, false), positive("daily.reward", 35)))) tell(p, "§aGörev ödülün cüzdanına eklendi.");
        } catch (Exception e) { storageError(e); }
    }
    private void claimEvent(Player p) {
        try {
            Progress d = data(p);
            // A completed event can still be claimed after the timer ends, until the next event.
            if (eventId.isEmpty() || !eventId.equals(d.eventId) || d.eventClaimed || d.eventCount < target(rotation, true)) {
                tell(p, "Alınabilecek etkinlik ödülün yok. /uzay etkinlik"); return;
            }
            if (transaction(p, v -> { v.eventClaimed = true; v.dust += positive("events.reward", 45); })) tell(p, "§dKozmik etkinlik ödülü cüzdanına eklendi!");
        } catch (Exception e) { storageError(e); }
    }
    private void startEvent(int type) {
        rotation = type; eventType = type; eventId = UUID.randomUUID().toString();
        eventEnd = System.currentTimeMillis() + duration() * 1000L;
        if (!saveEvent()) { eventType = -1; return; }
        bar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        Bukkit.broadcastMessage("§b✦ §d" + labels[type] + " başladı! §7Hedef: " + target(type, true) + " • §f/uzay etkinlik");
    }
    private void stopEvent() {
        if (bar != null) { bar.removeAll(); bar = null; }
        eventType = -1; nextEvent = System.currentTimeMillis() + interval() * 1000L;
        saveEvent();
    }
    private void restoreEvent() {
        java.io.File file = new java.io.File(getDataFolder(), "event.yml");
        if (!file.exists()) return;
        try {
            var y = new org.bukkit.configuration.file.YamlConfiguration(); y.load(file);
            rotation = Math.max(0, Math.min(2, y.getInt("type")));
            eventId = y.getString("id", ""); eventEnd = y.getLong("end"); nextEvent = y.getLong("next", nextEvent);
            if (y.getBoolean("active") && System.currentTimeMillis() < eventEnd) {
                eventType = rotation; bar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SEGMENTED_10);
            }
        } catch (Exception e) { storageError(e); }
    }
    private boolean saveEvent() {
        try {
            var y = new org.bukkit.configuration.file.YamlConfiguration();
            y.set("id", eventId); y.set("type", rotation); y.set("active", eventType >= 0); y.set("end", eventEnd); y.set("next", nextEvent);
            var tmp = getDataFolder().toPath().resolve("event.tmp");
            java.nio.file.Files.writeString(tmp, y.saveToString());
            java.nio.file.Files.move(tmp, getDataFolder().toPath().resolve("event.yml"), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) { storageError(e); return false; }
    }
    private void tick() {
        long now = System.currentTimeMillis();
        if (eventType < 0) {
            if (getConfig().getBoolean("events.enabled", true) && now >= nextEvent && !Bukkit.getOnlinePlayers().isEmpty()) startEvent((rotation + 1) % 3);
            return;
        }
        if (now >= eventEnd) {
            Bukkit.broadcastMessage("§b✦ §d" + labels[eventType] + " sona erdi! §7Tamamlayanlar: /uzay odul"); stopEvent(); return;
        }
        bar.setTitle("§d✦ " + labels[eventType] + " §f• " + (eventEnd - now) / 60_000 + " dk • /uzay etkinlik");
        bar.setProgress(Math.max(0, Math.min(1, (eventEnd - now) / (duration() * 1000.0))));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (combatArea(p, p.getLocation())) {
                bar.addPlayer(p);
                if (p.getWorld().getFullTime() % 5 == 0) p.spawnParticle(Particle.END_ROD, p.getLocation().add(0, 3, 0), 3, 3, 1, 3, .015);
            } else bar.removePlayer(p);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void superiorGenerated(IslandGenerateBlockEvent e) {
        if (!e.isPlaceBlock()) return;
        Material material = Material.matchMaterial(e.getBlock().getGlobalKey());
        if (material == null || !rock(material)) return;
        if (generated.size() >= 100_000) generated.clear();
        generated.put(e.getLocation().clone(), material);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void formed(BlockFormEvent e) {
        if (e instanceof EntityBlockFormEvent) return;
        Material material = e.getNewState().getType();
        if (material == Material.COBBLESTONE || material == Material.STONE || material == Material.BASALT || material.name().endsWith("_ORE")) {
            if (generated.size() >= 100_000) generated.clear();
            generated.put(e.getBlock().getLocation(), material);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void placed(BlockPlaceEvent e) { generated.remove(e.getBlock().getLocation()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent e) { e.getBlocks().forEach(b -> { generated.remove(b.getLocation()); generated.remove(b.getRelative(e.getDirection()).getLocation()); }); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void piston(BlockPistonRetractEvent e) { e.getBlocks().forEach(b -> { generated.remove(b.getLocation()); generated.remove(b.getRelative(e.getDirection()).getLocation()); }); }
    @EventHandler public void unload(ChunkUnloadEvent e) {
        generated.keySet().removeIf(l -> l.getWorld().equals(e.getWorld()) && (l.getBlockX() >> 4) == e.getChunk().getX() && (l.getBlockZ() >> 4) == e.getChunk().getZ());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void mined(BlockBreakEvent e) {
        Player p = e.getPlayer(); Block b = e.getBlock();
        Material original = b.getType(); Material marker = generated.remove(b.getLocation());
        if (!survival(p) || !island(p, b.getLocation())) return;
        if (marker == original && e.isDropItems()) progress(p, 0);
        Gear held = Gear.identify(p.getInventory().getItemInMainHand(), itemKey);
        if (held == Gear.METEOR_PICK && p.isSneaking() && rock(original) && !excavating.contains(p.getUniqueId())) {
            Location origin = b.getLocation(); int toolSlot = p.getInventory().getHeldItemSlot();
            boolean horizontal = Math.abs(p.getLocation().getPitch()) > 45;
            boolean alongX = Math.abs(p.getLocation().getDirection().getX()) > Math.abs(p.getLocation().getDirection().getZ());
            Bukkit.getScheduler().runTask(this, () -> {
                if (!p.isOnline() || !survival(p) || !p.getWorld().equals(origin.getWorld()) || p.getLocation().distanceSquared(origin) > 64 || toolSlot != p.getInventory().getHeldItemSlot() || Gear.identify(p.getInventory().getItemInMainHand(), itemKey) != Gear.METEOR_PICK) return;
                excavating.add(p.getUniqueId());
                try {
                    for (int a = -1; a <= 1; a++) for (int c = -1; c <= 1; c++) {
                        if (a == 0 && c == 0) continue;
                        if (Gear.identify(p.getInventory().getItemInMainHand(), itemKey) != Gear.METEOR_PICK) return;
                        Block extra = origin.clone().add(horizontal ? a : alongX ? 0 : a, horizontal ? 0 : c, horizontal ? c : alongX ? a : 0).getBlock();
                        if (extra.getType() == original && island(p, extra.getLocation())) p.breakBlock(extra);
                    }
                } finally { excavating.remove(p.getUniqueId()); }
            });
        }
    }
    private boolean rock(Material m) {
        return switch (m) {
            case STONE, COBBLESTONE, DEEPSLATE, COBBLED_DEEPSLATE, ANDESITE, DIORITE, GRANITE, BASALT, BLACKSTONE, NETHERRACK, END_STONE -> true;
            default -> m.name().endsWith("_ORE");
        };
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void harvest(BlockDropItemEvent e) {
        Player p = e.getPlayer(); var state = e.getBlockState();
        if (!survival(p) || !island(p, state.getLocation()) || !(state.getBlockData() instanceof Ageable age) || age.getAge() != age.getMaximumAge()) return;
        Material seed = switch (state.getType()) {
            case WHEAT -> Material.WHEAT_SEEDS; case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO; case BEETROOTS -> Material.BEETROOT_SEEDS;
            default -> null;
        };
        if (seed == null) return;
        progress(p, 2);
        if (Gear.identify(p.getInventory().getItemInMainHand(), itemKey) != Gear.ORBIT_HOE) return;
        Block b = e.getBlock();
        if (!b.getType().isAir() || b.getRelative(0, -1, 0).getType() != Material.FARMLAND) return;
        for (Item drop : e.getItems()) if (drop.getItemStack().getType() == seed) {
            // Replant only through a real placement protection event; use one actual drop as seed.
            var old = b.getState(); Ageable fresh = (Ageable) Bukkit.createBlockData(state.getType()); fresh.setAge(0);
            b.setBlockData(fresh, false);
            BlockPlaceEvent place = new BlockPlaceEvent(b, old, b.getRelative(0, -1, 0), new ItemStack(seed), p, true, EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(place);
            if (place.isCancelled() || !place.canBuild()) { old.update(true, false); return; }
            ItemStack stack = drop.getItemStack();
            if (stack.getAmount() == 1) { e.getItems().remove(drop); drop.remove(); }
            else { stack.setAmount(stack.getAmount() - 1); drop.setItemStack(stack); }
            b.setBlockData(fresh); return;
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void spawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.PATROL || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.RAID)
            e.getEntity().getPersistentDataContainer().set(naturalKey, PersistentDataType.BYTE, (byte) 1);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void death(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null || !(e.getEntity() instanceof Monster) || !combatArea(p, e.getEntity().getLocation())) return;
        if (e.getEntity().getPersistentDataContainer().has(naturalKey, PersistentDataType.BYTE) || e.getEntity().getPersistentDataContainer().has(new NamespacedKey("novadungeon", "level"), PersistentDataType.INTEGER)) progress(p, 1);
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof Monster) || !survival(p) || !combatArea(p, e.getEntity().getLocation()) || p.getAttackCooldown() < .95) return;
        Gear gear = Gear.identify(p.getInventory().getItemInMainHand(), itemKey);
        if (gear == Gear.PULSAR_SWORD || gear == Gear.NOVA_SWORD) {
            e.setDamage(e.getDamage() + (gear == Gear.NOVA_SWORD ? 4 : 2));
            p.spawnParticle(Particle.END_ROD, e.getEntity().getLocation().add(0, 1, 0), 8, .3, .4, .3, .02);
        }
    }
    @EventHandler public void interact(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer(); Gear gear = Gear.identify(e.getItem(), itemKey);
        if (gear == null || !survival(p) || !combatArea(p, p.getLocation())) return;
        // Air interactions can be pre-cancelled by Bukkit; respect explicit block denials.
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && (e.useInteractedBlock() == Event.Result.DENY || e.useItemInHand() == Event.Result.DENY)) return;
        PotionEffectType effect; int ticks, amplifier, seconds;
        switch (gear) {
            case LUNAR_PICK -> { effect = PotionEffectType.FAST_DIGGING; ticks = 160; amplifier = 1; seconds = 30; }
            case NOVA_SWORD -> { effect = PotionEffectType.INCREASE_DAMAGE; ticks = 120; amplifier = 0; seconds = 35; }
            case VOID_RELIC -> { effect = PotionEffectType.SLOW_FALLING; ticks = 240; amplifier = 0; seconds = 60; }
            default -> { return; }
        }
        e.setCancelled(true);
        Map<Gear, Long> times = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(Gear.class));
        long now = System.currentTimeMillis(), ready = times.getOrDefault(gear, 0L);
        if (now < ready) { tell(p, "§eYetenek " + ((ready - now + 999) / 1000) + " saniye sonra hazır."); return; }
        times.put(gear, now + seconds * 1000L);
        p.addPotionEffect(new PotionEffect(effect, ticks, amplifier));
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 18, .5, .5, .5, .02);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, .6f, 1.8f);
    }
    @EventHandler public void craft(PrepareItemCraftEvent e) {
        for (ItemStack item : e.getInventory().getMatrix()) if (Gear.identify(item, itemKey) != null) { e.getInventory().setResult(null); return; }
    }
    @EventHandler public void grind(PrepareGrindstoneEvent e) {
        if (Gear.identify(e.getInventory().getItem(0), itemKey) != null || Gear.identify(e.getInventory().getItem(1), itemKey) != null) e.setResult(null);
    }
    @EventHandler public void smith(PrepareSmithingEvent e) {
        for (int i = 0; i < 3; i++) if (Gear.identify(e.getInventory().getItem(i), itemKey) != null) { e.setResult(null); return; }
    }
    @EventHandler public void join(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!e.getPlayer().isOnline()) return;
            try { deliver(e.getPlayer()); } catch (Exception ex) { storageError(ex); }
            tell(e.getPlayer(), "§bUzay yolculuğun hazır! §f/uzay §7• Ada: §f/is"); sendPack(e.getPlayer(), false);
        }, 60);
    }
    @EventHandler public void quit(PlayerQuitEvent e) {
        if (bar != null) bar.removePlayer(e.getPlayer());
        try { store.release(e.getPlayer().getUniqueId()); } catch (Exception ex) { storageError(ex); }
        // Keep short cooldowns across relogs, prune expired entries during quit.
        long now = System.currentTimeMillis(); cooldowns.values().forEach(m -> m.values().removeIf(t -> t <= now));
        cooldowns.values().removeIf(Map::isEmpty);
    }
    private void sendPack(Player p, boolean requested) {
        if (packUrl.isEmpty()) {
            if (requested) tell(p, "§cGörünüm paketi şu an hazır değil. Yöneticiye bildir."); return;
        }
        p.addResourcePack(packId, packUrl, HexFormat.of().parseHex(packHash),
                "Uzay ekipmanlarını görmek için Nova Cosmos paketini kabul et. Otomatik indirilir.",
                getConfig().getBoolean("resource-pack.required", true));
    }
    private void setupPack() {
        if (!getConfig().getBoolean("resource-pack.enabled", true)) return;
        try {
            String url = getConfig().getString("resource-pack.url", "").trim();
            var uri = java.net.URI.create(url);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null)
                throw new IllegalArgumentException("resource-pack.url geçerli HTTP/HTTPS adresi olmalı");
            String hash = getConfig().getString("resource-pack.sha1", "").trim();
            if (getConfig().getBoolean("resource-pack.host.enabled", true)) {
                try (var in = getResource("novacosmos.zip")) {
                    if (in == null) throw new IllegalStateException("JAR içinde novacosmos.zip yok");
                    packHost = new PackHost(in.readAllBytes(), getConfig().getString("resource-pack.host.bind", "127.0.0.1"),
                            getConfig().getInt("resource-pack.host.port", 8088));
                    hash = packHost.sha1;
                }
            }
            if (!hash.matches("[0-9a-fA-F]{40}")) throw new IllegalArgumentException("Paket SHA-1 değeri geçersiz");
            packUrl = url; packHash = hash;
            getLogger().info("Otomatik görünüm paketi hazır: " + packUrl + " • SHA-1 " + packHash);
        } catch (Exception ex) {
            if (packHost != null) { packHost.close(); packHost = null; }
            getLogger().log(Level.SEVERE, "Otomatik kaynak paketi başlatılamadı", ex);
        }
    }
    @EventHandler public void packStatus(PlayerResourcePackStatusEvent e) {
        if (!packId.equals(e.getID())) return;
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> tell(e.getPlayer(), "§bUzay görünümleri yüklendi! Özel ekipmanını eline al.");
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD -> {
                if (getConfig().getBoolean("resource-pack.required", true))
                    e.getPlayer().kickPlayer("Nova Cosmos görünüm paketi indirilemedi. Sunucuya yeniden bağlan; sorun sürerse yöneticiye bildir.");
                else tell(e.getPlayer(), "§eGörünüm paketi yüklenemedi. Yeniden dene: /uzay paket");
            }
            default -> { }
        }
    }
    private void orbitStars() {
        if (!getConfig().getBoolean("visuals.orbit-stars", true)) return;
        starPhase = (starPhase + .24) % (Math.PI * 2);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || p.isInvisible() || p.getGameMode() == GameMode.SPECTATOR) continue;
            Gear held = Gear.identify(p.getInventory().getItemInMainHand(), itemKey);
            if (held == null) continue;
            Color color = switch (held) {
                case LUNAR_PICK, VOID_RELIC -> Color.fromRGB(90, 235, 255);
                case METEOR_PICK -> Color.fromRGB(255, 190, 60);
                case PULSAR_SWORD, NOVA_SWORD -> Color.fromRGB(195, 110, 255);
                case ORBIT_HOE -> Color.fromRGB(100, 255, 165);
            };
            Location center = p.getLocation();
            for (int i = 0; i < 2; i++) {
                double angle = starPhase + i * Math.PI;
                Location star = center.clone().add(Math.cos(angle) * .75,
                        1.1 + Math.sin(angle * 2) * .3, Math.sin(angle) * .75);
                for (Player viewer : p.getWorld().getPlayers()) {
                    if (!viewer.canSee(p) || viewer.getLocation().distanceSquared(center) > 576) continue;
                    viewer.spawnParticle(Particle.END_ROD, star, 1, 0, 0, 0, .003);
                    viewer.spawnParticle(Particle.REDSTONE, star, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(color, .65f));
                }
            }
        }
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "menu" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("admin")) {
            if (!sender.hasPermission("novacosmos.admin")) { tell(sender, "§cYetkin yok."); return true; }
            if (args.length == 3 && args[1].equals("baslat")) {
                int type = Arrays.asList(types).indexOf(args[2]);
                if (type < 0 || eventType >= 0) tell(sender, "Geçerli tür: mining/combat/farming. Önce aktif etkinliği bitir.");
                else { startEvent(type); tell(sender, "Etkinlik başlatıldı."); }
            } else if (args.length == 2 && args[1].equals("bitir")) { stopEvent(); tell(sender, "Etkinlik durduruldu."); }
            else tell(sender, "/uzay admin baslat <mining|combat|farming> • /uzay admin bitir");
            return true;
        }
        if (!(sender instanceof Player p)) { tell(sender, "Oyunda /uzay; konsolda /uzay admin"); return true; }
        switch (sub) {
            case "menu" -> menu(p);
            case "baslangic" -> starter(p);
            case "gorev" -> { for (int i = 0; i < 3; i++) claim(p, i); menu(p); }
            case "odul" -> claimEvent(p);
            case "paket" -> sendPack(p, true);
            case "teslim" -> { try { if (deliver(p)) tell(p, "§aBekleyen teslimatların tamamlandı."); } catch (Exception e) { storageError(e); } }
            case "etkinlik" -> {
                if (eventType < 0) tell(p, "Etkinlik bekleniyor. Sonraki olay: " + Math.max(0, (nextEvent - System.currentTimeMillis()) / 60000) + " dakika.");
                else try {
                    Progress d = data(p); d.event(eventId);
                    tell(p, "§d" + labels[eventType] + " §7• " + d.eventCount + "/" + target(eventType, true) + " • Ödül: " + positive("events.reward", 45) + " toz");
                    tell(p, "Jeneratör kaz / doğal canavar kes / olgun ürün hasat et. Hedef tamamlanınca /uzay odul");
                } catch (Exception e) { storageError(e); }
            }
            default -> tell(p, "/uzay • baslangic • gorev • etkinlik • odul • paket • teslim");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        List<String> options = new ArrayList<>();
        if (a.length == 1) { options.addAll(List.of("baslangic", "gorev", "etkinlik", "odul", "paket", "teslim")); if (s.hasPermission("novacosmos.admin")) options.add("admin"); }
        if (s.hasPermission("novacosmos.admin") && a[0].equals("admin")) {
            if (a.length == 2) options.addAll(List.of("baslat", "bitir"));
            if (a.length == 3 && a[1].equals("baslat")) options.addAll(Arrays.asList(types));
        }
        return options.stream().filter(v -> v.startsWith(a[a.length - 1].toLowerCase(Locale.ROOT))).toList();
    }
}
