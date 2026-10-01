package org.nova.novaDungeon.command;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.nova.novaDungeon.gui.AdminGUI;

public class AdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {

        if (!(s instanceof Player)) return false;

        AdminGUI.open((Player) s);
        return true;
    }
}