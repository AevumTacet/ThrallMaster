package com.thrallmaster;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

   public static Main plugin;
   public static FileConfiguration config;
   public static ThrallManager manager;

   @Override
   public void onEnable() {
      plugin = this;
      saveDefaultConfig();
      config = getConfig();

      ThrallManager.logger = getLogger();
      manager = new ThrallManager();
      manager.registerAllEntities(this.getServer().getWorlds().get(0));

      this.getServer().getPluginManager().registerEvents(manager, this);
      getLogger().info("Thrall Master plugin enabled.");
   }


   @Override
   public void onDisable() {
      super.onDisable();
      ThrallManager.saveNBT();
   }

   public static void reload() {
      plugin.reloadConfig();
      config = plugin.getConfig();
   }
}