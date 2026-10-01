package com.Bannan.bannanDefaultLobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

public class GameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("remove")) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!(entity instanceof Player)) {
                        entity.remove();
                    }
                }
            }
            commandSender.sendMessage("All entities removed.");
            return true;
        }
        if (!(commandSender instanceof Player)){
            commandSender.sendMessage("Only Players Can Use This");
            return true;
        }
        Player p = (Player) commandSender;
        Location loc = p.getLocation().clone();
        loc.setPitch(0f);
        Villager villager = (Villager) p.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.getPersistentDataContainer().set(BannanDefaultLobby.NpcTagKey, PersistentDataType.BOOLEAN, true);
        return true;
    }
}
