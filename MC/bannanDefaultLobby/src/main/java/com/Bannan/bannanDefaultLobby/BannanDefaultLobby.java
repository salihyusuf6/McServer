package com.Bannan.bannanDefaultLobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class BannanDefaultLobby extends JavaPlugin {
    static String SpawnWorldName;
    static Vector DefaultSpawnPosition;
    static Location DefaultSpawnLocation = null;
    static Plugin MyPlugin;
    static NamespacedKey LoginCheck;
    static NamespacedKey NpcTagKey;
    static NamespacedKey SkyblockButtonKey;
    static String BackendServerURL;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        MyPlugin = this;
        Bukkit.getPluginManager().registerEvents(new MyEventListener(), this);
        DefaultSpawnPosition = getConfig().getVector("DefaultSpawnPosition");
        SpawnWorldName = getConfig().getString("SpawnWorldName");
        LoginCheck = new NamespacedKey(this,"IsLogined");
        NpcTagKey = new NamespacedKey(this,"GameNpc");
        SkyblockButtonKey = new NamespacedKey(this,"SkyblockButton");
        BackendServerURL = getConfig().getString("BackendServerURL");
        getServer().getMessenger().registerOutgoingPluginChannel(this,"BungeeCord");
        LoginManager l = new LoginManager(this);
        getCommand("login").setExecutor(l);
        getCommand("Login").setExecutor(l);
        getCommand("l").setExecutor(l);
        getCommand("L").setExecutor(l);
        getCommand("game").setExecutor(new GameCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
