package com.Bannan.bannanDefaultLobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class LoginManager implements CommandExecutor {
    public LoginManager(BannanDefaultLobby bannanDefaultLobby) {
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length != 1)
        {
            return false;
        }
        if (!(commandSender instanceof Player)){
            commandSender.sendMessage("Only Players Can Use This");
            return true;
        }
        Player p = (Player) commandSender;
        PersistentDataContainer con = p.getPersistentDataContainer();
        Boolean temp = con.get(BannanDefaultLobby.LoginCheck,PersistentDataType.BOOLEAN);
        if (temp)
        {
            p.sendMessage(Component.text("You Already Logined.", NamedTextColor.RED));
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(BannanDefaultLobby.MyPlugin,()->{
            //Async Part
            String msg = "";
            try{
                msg = Network.LoginCheckEvent(p.getName(),args[0],String.format("%s/Login",BannanDefaultLobby.BackendServerURL));
            }
            catch (Exception e){
                p.sendMessage(e.toString());
            }
            String finalMsg = msg;
            Bukkit.getScheduler().runTask(BannanDefaultLobby.MyPlugin,()->{
                //Synce Part
                if (finalMsg.toLowerCase().equals("true".toLowerCase())){
                    p.sendMessage(Component.text("Login Succuses", NamedTextColor.GREEN));
                    con.set(BannanDefaultLobby.LoginCheck,PersistentDataType.BOOLEAN,true);
                }
                else {
                    p.sendMessage(Component.text("Wrong Password",NamedTextColor.RED));
                }

            });
        });
        return true;
    }
}
