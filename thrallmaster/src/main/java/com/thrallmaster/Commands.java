package com.thrallmaster;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;


public class Commands
{
    static ThrallManager manager = Main.manager;

    public static void registerCommands(Plugin plugin)
    {
        CommandAPICommand base = new CommandAPICommand("thrall")
        
            .withSubcommand(new CommandAPICommand("spawn")
                .withPermission("thrall.spawn")
                .withOptionalArguments(new PlayerArgument("owner"))
                .withOptionalArguments(new LocationArgument("location"))
                .executesPlayer((player, args) ->
                {
                    Player owner = (Player) args.get("owner");
                    Location location = (Location) args.get("location");

                    if (owner == null)
                    {
                        owner = player;
                    }
                    if (location == null)
                    {
                        location = player.getLocation();
                    }

                    manager.spawnThrall(location, owner);
                })
            );
                                
                                    
        base.register();
    }    

}
