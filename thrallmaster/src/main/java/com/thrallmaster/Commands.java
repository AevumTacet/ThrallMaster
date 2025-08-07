package com.thrallmaster;

import java.util.Comparator;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.thrallmaster.Utils.ThrallUtils;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;

public class Commands {
    static ThrallManager manager = Main.manager;

    public static void registerCommands(Plugin plugin) {
        CommandAPICommand base = new CommandAPICommand("thrall")
                .withSubcommand(reloadCommand())
                .withSubcommand(spawnCommand())
                .withSubcommand(allyCommand())
                .withSubcommand(transferCommand())
                .withSubcommand(selectAllCommand())
                .withSubcommand(listCommand());

        base.register();
    }

    private static CommandAPICommand reloadCommand() {
        return new CommandAPICommand("reload")
                .withPermission("thrall.reload")
                .executes((sender, args) -> {
                    sender.sendMessage("Reloading ThrallMaster configuration.");
                    Main.reload();
                });
    }

    private static CommandAPICommand spawnCommand() {
        return new CommandAPICommand("spawn")
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
                });
    }

    private static CommandAPICommand allyCommand() {
        return new CommandAPICommand("ally")
                .withSubcommand(allyAddCommand())
                .withSubcommand(allyRemoveCommand());
    }

    private static CommandAPICommand allyAddCommand() {
        return new CommandAPICommand("add")
                .withArguments(new OfflinePlayerArgument("player"))
                .executesPlayer((player, args) -> {
                    Player target = (Player) args.get("player");
                    if (target == null) {
                        throw CommandAPI
                                .failWithString("Cannot assign ally because player doesn't exist.");
                    }

                    manager.getOwnerData(player.getUniqueId()).addAlly(target.getUniqueId());
                    player.sendMessage(target.getName() + " is now an ally!");
                });
    }

    private static CommandAPICommand allyRemoveCommand() {
        return new CommandAPICommand("remove")
                .withArguments(new PlayerArgument("player"))
                .executesPlayer((player, args) -> {
                    Player target = (Player) args.get("player");
                    if (target == null) {
                        throw CommandAPI
                                .failWithString("Cannot remove ally because player doesn't exist.");
                    }

                    manager.getOwnerData(player.getUniqueId()).removeAlly(target.getUniqueId());
                    player.sendMessage(target.getName() + " is no longer an ally.");
                });
    }

    private static CommandAPICommand transferCommand() {
        return new CommandAPICommand("transfer")
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
                });
    }

    private static CommandAPICommand selectAllCommand() {
        return new CommandAPICommand("select_all")
                .executesPlayer((player, args) -> {
                    manager.getThralls(player.getUniqueId())
                            .forEach(state -> state.setSelected(true));
                    player.sendMessage(
                            "Selecting " + manager.getOwnerData(player.getUniqueId()).getCount() + " Thralls.");
                });
    }

    private static CommandAPICommand listCommand() {
        return new CommandAPICommand("list")
                .executesPlayer((player, args) -> {
                    player.sendMessage(" ");
                    manager.getThralls(player.getUniqueId())
                            .filter(state -> state.isValidEntity())
                            .sorted(Comparator.comparingDouble(state -> ThrallUtils.distanceToOwner(state)))
                            .forEach(state -> {
                                LivingEntity entity = (LivingEntity) state.getEntity();
                                int distance = (int) ThrallUtils.distanceToOwner(state);

                                player.sendMessage(entity.getName() + " " + (int) entity.getHealth() + " ❤" + ":");
                                player.sendMessage("- " + state.getBehavior().getBehaviorName());
                                player.sendMessage("- " + distance + " m");

                            });
                });
    }
}
