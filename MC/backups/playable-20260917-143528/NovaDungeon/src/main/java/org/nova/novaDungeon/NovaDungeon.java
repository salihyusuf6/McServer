package org.nova.novaDungeon;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nova.novaDungeon.command.AdminCommand;
import org.nova.novaDungeon.listener.*;
import org.nova.novaDungeon.manager.BossbarManager;
import org.nova.novaDungeon.manager.MobManager;

public final class NovaDungeon extends JavaPlugin {

    private static NovaDungeon instance;

    private MobManager mobManager;
    private BossbarManager bossbarManager;

    @Override
    public void onEnable() {
        instance = this;

        mobManager = new MobManager();
        bossbarManager = new BossbarManager();

        // LISTENERS
        Bukkit.getPluginManager().registerEvents(new SpawnListener(), this);
        Bukkit.getPluginManager().registerEvents(new MoveListener(mobManager), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new DamageListener(), this);
        Bukkit.getPluginManager().registerEvents(new FireFixListener(), this);

        // COMMAND
        getCommand("dungeonadmin").setExecutor(new AdminCommand());

        getLogger().info("NovaDungeon aktif!");
    }

    public static NovaDungeon getInstance() {
        return instance;
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public BossbarManager getBossbarManager() {
        return bossbarManager;
    }
}