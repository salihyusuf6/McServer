package org.nova.novaCrates.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.nova.novaCrates.crate.CrateGUI;
import org.nova.novaCrates.crate.CrateManager;

public class CrateCommand implements CommandExecutor {

    private final CrateManager manager;
    private final CrateGUI gui;

    public CrateCommand(CrateManager manager) {
        this.manager = manager;
        this.gui = new CrateGUI(manager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) return true;

        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 2) { p.sendMessage("§c/crate set <isim>"); return true; }
                org.bukkit.block.Block targetBlock = p.getTargetBlockExact(5);
                if (targetBlock == null) { p.sendMessage("§cBir bloğa/kasaya bakmalısın!"); return true; }
                manager.setCrate(args[1].toLowerCase(), targetBlock.getLocation());
                p.sendMessage("§aKasa '" + args[1].toLowerCase() + "' baktığın bloğa ayarlandı!");
            }
            case "give" -> {
                if (args.length < 3) { p.sendMessage("§c/crate give <oyuncu> <crate> [miktar]"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { p.sendMessage("§cOyuncu bulunamadı!"); return true; }
                int amount = 1;
                if (args.length >= 4) try { amount = Integer.parseInt(args[3]); } catch (Exception ignored) {}
                org.bukkit.inventory.ItemStack key = manager.createKey(args[2].toLowerCase());
                if (key == null) { p.sendMessage("§c" + args[2] + " adında bir kasa bulunamadı!"); return true; }
                key.setAmount(amount);
                target.getInventory().addItem(key);
                p.sendMessage("§a" + args[2].toLowerCase() + " crate key verildi!");
            }
            case "preview" -> {
                if (args.length < 2) { p.sendMessage("§c/crate preview <crate>"); return true; }
                gui.preview(p, args[1].toLowerCase());
            }
            default -> p.sendMessage("§cKullanım: /crate set/give/preview");
        }

        return true;
    }
}