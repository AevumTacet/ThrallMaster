package com.thrallmaster;

import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.plugin.Plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;

public class Commands {
    static ThrallManager manager = Main.manager;

    public static void registerCommands(Plugin plugin) {
        CommandAPICommand base = new CommandAPICommand("thrall")
                .withSubcommand(new CommandAPICommand("reload")
                        .withPermission("thrall.reload")
                        .executes((sender, args) -> {
                            sender.sendMessage("Reloading ThrallMaster configuration.");
                            Main.reload();
                        }))

                .withSubcommand(new CommandAPICommand("spawn")
                        .withPermission("thrall.spawn")
                        .withOptionalArguments(new PlayerArgument("owner"))
                        .withOptionalArguments(new LocationArgument("location"))
                        .executes((sender, args) -> {
                            Player owner = (Player) args.get("owner");
                            Location location = (Location) args.get("location");

                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                if (owner == null) {
                                    owner = player;
                                }
                                if (location == null) {
                                    location = player.getLocation();
                                }
                            } else {
                                if (owner == null) {
                                    throw CommandAPI.failWithString(
                                            "An owner must be passed when invoking this command using the console.");
                                }
                                if (location == null) {
                                    location = owner.getLocation();
                                }
                            }

                            manager.spawnThrall(location, owner);
                        }))

                .withSubcommand(new CommandAPICommand("ally")
                        .withSubcommand(new CommandAPICommand("add")
                                .withArguments(new OfflinePlayerArgument("player"))
                                .executesPlayer((player, args) -> {
                                    Player target = (Player) args.get("player");
                                    if (target == null) {
                                        throw CommandAPI
                                                .failWithString("Cannot assign ally because player doesn't exist.");
                                    }

                                    manager.getOwnerData(player.getUniqueId()).addAlly(target.getUniqueId());
                                    ;
                                    player.sendMessage(target.getName() + " is now an ally!");
                                }))
                        .withSubcommand(new CommandAPICommand("remove")
                                .withArguments(new PlayerArgument("player"))
                                .executesPlayer((player, args) -> {
                                    Player target = (Player) args.get("player");
                                    if (target == null) {
                                        throw CommandAPI
                                                .failWithString("Cannot remove ally because player doesn't exist.");
                                    }

                                    manager.getOwnerData(player.getUniqueId()).removeAlly(target.getUniqueId());
                                    ;
                                    player.sendMessage(target.getName() + " is no longer an ally.");
                                })))

                .withSubcommand(new CommandAPICommand("transfer")
                        .withArguments(new PlayerArgument("player"))
                        .executesPlayer((player, args) -> {
                            Player target = (Player) args.get("player");
                            if (target == null) {
                                throw CommandAPI
                                        .failWithString("Cannot transfer Thralls because target player doesn't exist.");
                            }

                            var selected = manager.getThralls(player.getUniqueId())
                                    .filter(state -> state.isSelected() && state.isValidEntity())
                                    .collect(Collectors.toList());
                            if (selected.size() == 0) {
                                throw CommandAPI.failWithString("You dont have Thralls selected.");
                            }

                            selected.forEach(state -> {
                                manager.unregister(state.getEntityID());
                                manager.registerThrall((AbstractSkeleton) state.getEntity(), target);
                            });

                            player.sendMessage(
                                    "You transferred " + selected.size() + " Thralls to " + target.getName());
                            target.sendMessage("You recieved " + selected.size() + " Thralls from " + player.getName());
                        }));

        base.register();
    }

}
