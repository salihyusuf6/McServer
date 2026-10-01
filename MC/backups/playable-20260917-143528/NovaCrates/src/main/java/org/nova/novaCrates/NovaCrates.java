package org.nova.novaCrates;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.nova.novaCrates.commands.CrateCommand;
import org.nova.novaCrates.crate.CrateManager;
import org.nova.novaCrates.listeners.CrateListener;

public class NovaCrates extends JavaPlugin {

    private static NovaCrates instance;
    private CrateManager crateManager;
    public static Economy econ;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        crateManager = new CrateManager(this);

        getServer().getPluginManager().registerEvents(new CrateListener(crateManager), this);

        getCommand("crate").setExecutor(new CrateCommand(crateManager));

        setupEconomy();

        getLogger().info("NovaCrates aktif!");
    }

    public static NovaCrates getInstance() {
        return instance;
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }
}