package com.thrallmaster;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.thrallmaster.IO.NBTExporter;
import com.thrallmaster.Protocols.ThrallProtocol;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;

public class Main extends JavaPlugin {

   public static Main plugin;
   public static FileConfiguration config;
   public static ThrallManager manager;
   public static NBTExporter saver;

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
      manager.restorePlayers();

      Commands.registerCommands(this);


      this.getServer().getPluginManager().registerEvents(manager, this);
      getLogger().info("Thrall Master plugin enabled.");
   }


   @Override
   public void onDisable() {
      super.onDisable();
      CommandAPI.onDisable();

      getLogger().info("Saving Thrall NBT state.");
      // getLogger().info(count + " Entitites saved.");

      manager.savePlayers(true);
   }

   public static void reload() {
      plugin.reloadConfig();
      config = plugin.getConfig();
   }
}