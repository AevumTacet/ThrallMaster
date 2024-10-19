package com.thrallmaster;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.thrallmaster.Protocols.ThrallProtocol;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

public class Main extends JavaPlugin {

   public static Main plugin;
   public static FileConfiguration config;
   public static ThrallManager manager;

   @Override
   public void onLoad() {
      super.onLoad();
      CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(true));
      ThrallProtocol.onLoad(this);
   }


   @Override
   public void onEnable() {
      plugin = this;
      config = getConfig();
      saveDefaultConfig();

      CommandAPI.onEnable();

      ThrallManager.logger = getLogger();
      manager = new ThrallManager();
      manager.registerAllEntities(this.getServer().getWorlds().get(0));
      manager.registerWhitelist();

      Commands.registerCommands(this);

      this.getServer().getPluginManager().registerEvents(manager, this);
      getLogger().info("Thrall Master plugin enabled.");
   }


   @Override
   public void onDisable() {
      super.onDisable();
      ThrallManager.saveNBT();
      CommandAPI.onDisable();
   }

   public static void reload() {
      plugin.reloadConfig();
      config = plugin.getConfig();
   }
}