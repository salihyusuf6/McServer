package org.nova.novaCrates;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.nova.novaCrates.commands.CrateCommand;
import org.nova.novaCrates.crate.*;
import org.nova.novaCrates.listeners.CrateListener;
public class NovaCrates extends JavaPlugin {
    private static NovaCrates instance;private CrateManager manager;private CrateVisuals visuals;public static Economy econ;
    public static NovaCrates getInstance(){return instance;}
    public CrateManager getCrateManager(){return manager;}
    public CrateVisuals visuals(){return visuals;}
    @Override public void onEnable(){
        instance=this;saveDefaultConfig();
        var provider=getServer().getServicesManager().getRegistration(Economy.class);
        if(provider==null){getLogger().severe("Vault ekonomi sağlayıcısı bulunamadı.");getServer().getPluginManager().disablePlugin(this);return;}
        econ=provider.getProvider();manager=new CrateManager(this);visuals=new CrateVisuals(this,manager);
        getServer().getPluginManager().registerEvents(new CrateListener(manager),this);
        CrateCommand command=new CrateCommand(manager);getCommand("crate").setExecutor(command);getCommand("crate").setTabCompleter(command);
        getServer().getScheduler().runTaskTimer(this,visuals::tick,20,5);
        getServer().getScheduler().runTaskLater(this,visuals::refresh,40);
    }
    @Override public void onDisable(){if(visuals!=null)visuals.clear();econ=null;}
}
