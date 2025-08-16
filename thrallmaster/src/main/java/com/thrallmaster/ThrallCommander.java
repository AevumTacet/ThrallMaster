package com.thrallmaster;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.MusicInstrument;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.util.RayTraceResult;

import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.FollowBehavior;
import com.thrallmaster.Behavior.HostileBehavior;
import com.thrallmaster.Behavior.IdleBehavior;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.ThrallUtils;

public class ThrallCommander {
    private static ThrallManager manager = Main.manager;

    public static void ToggleSelection(Player player) {
        UUID playerID = player.getUniqueId();
        Location eyeLocation = player.getEyeLocation();
        RayTraceResult rayTraceResult = player.getWorld()
                .rayTrace(eyeLocation, eyeLocation.getDirection(), 40, FluidCollisionMode.NEVER, true, 1.2, e -> {
                    return (e instanceof LivingEntity) && (e != player) && ThrallUtils.isThrall(e);
                });

        if (rayTraceResult != null && rayTraceResult.getHitEntity() != null) {
            Entity entity = rayTraceResult.getHitEntity();
            ThrallState state = manager.getThrall(player, entity.getUniqueId());

            if (state != null) {
                state.setSelected(!state.isSelected());
            }
        } else {
            manager.getThralls(playerID).forEach(x -> x.setSelected(false));
        }
    }

    public static void CommandSelection(Player player, Material itemMaterial) {
        UUID playerID = player.getUniqueId();
        Location eyeLocation = player.getEyeLocation();

        List<ThrallState> selected = manager.getThralls(playerID)
                .filter(x -> x.isSelected() && x.isValidEntity())
                .collect(Collectors.toList());

        if (selected.size() == 0) {
            return;
        }

        RayTraceResult rayTraceResult = player.getWorld()
                .rayTrace(eyeLocation, eyeLocation.getDirection(), 100, FluidCollisionMode.ALWAYS, true, 1.0, e -> {
                    return (e instanceof LivingEntity) && (e != player) && !ThrallUtils.isThrall(e);
                });

        if (rayTraceResult != null) {
            Block block = rayTraceResult.getHitBlock();
            Entity entity = rayTraceResult.getHitEntity();

            if (entity != null) {
                selected.forEach(state -> {
                    Behavior oldBehavior = state.getBehavior();
                    state.setAttackMode(entity);
                    state.setBehavior(new HostileBehavior(state.getEntityID(), state, oldBehavior));
                    player.getWorld().playSound(state.getEntity().getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                    state.setSelected(true);
                });

                player.getWorld().spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1, 0), 20, 0.1, 0.1, 0.1,
                        0.05);
                return;
            }

            else if (block != null) {
                selected.forEach(state -> {
                    state.setBehavior(new IdleBehavior(state.getEntityID(), state, block.getLocation().add(0, 1, 0)));
                    player.getWorld().playSound(state.getEntity().getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                    state.setSelected(true);
                });

                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0, 1, 0), 20, 0.1, 0.1,
                        0.1, 0.1);
            }
        }
    }

    public static void HornCommand(Player player) {
        UUID playerID = player.getUniqueId();
        ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
        if (meta instanceof MusicInstrumentMeta) {
            MusicInstrumentMeta instrumentMeta = (MusicInstrumentMeta) meta;
            MusicInstrument instrument = instrumentMeta.getInstrument();
            List<ThrallState> selected = manager.getThralls(playerID).filter(x -> x.isSelected() && x.isValidEntity())
                    .collect(Collectors.toList());

            if (selected.size() == 0) {
                return;
            }

            if (instrument == MusicInstrument.PONDER_GOAT_HORN) {
                boolean allFollow = selected.stream().allMatch(state -> state.getBehavior() instanceof FollowBehavior);
                if (allFollow) {
                    ThrallUtils.notifyPlayer(player,
                            String.format(Settings.BEHAVIOR_CHANGED_MSG_MULTI, selected.size(), Settings.IDLE_NAME));
                    selected.forEach(state -> state.setBehavior(new IdleBehavior(state.getEntityID(), state)));
                } else {
                    ThrallUtils.notifyPlayer(player,
                            String.format(Settings.BEHAVIOR_CHANGED_MSG_MULTI, selected.size(),
                                    Settings.FOLLOW_NAME));
                    selected.forEach(state -> state.setBehavior(new FollowBehavior(state.getEntityID(), state)));
                }
            } else if (instrument == MusicInstrument.SEEK_GOAT_HORN) {
                boolean allHostile = selected.stream()
                        .allMatch(state -> state.aggressionState == AggressionState.HOSTILE);
                if (allHostile) {
                    ThrallUtils.notifyPlayer(player,
                            String.format(Settings.BEHAVIOR_CHANGED_MSG_MULTI, selected.size(),
                                    Settings.DEFEND_VERB));
                    selected.forEach(state -> state.aggressionState = AggressionState.DEFENSIVE);
                } else {
                    ThrallUtils.notifyPlayer(player,
                            String.format(Settings.BEHAVIOR_CHANGED_MSG_MULTI, selected.size(),
                                    Settings.AGGRESIVE_VERB));
                    selected.forEach(state -> state.aggressionState = AggressionState.HOSTILE);
                }
            }
        }
    }

    public static void MultiSelect(Player player) {
        RayTraceResult rayTraceResult = player.rayTraceBlocks(40);

        if (rayTraceResult != null) {
            Block block = rayTraceResult.getHitBlock();

            if (block != null) {
                double selectRadius = 5;
                player.getWorld().getNearbyEntities(block.getLocation(), selectRadius, selectRadius, selectRadius)
                        .stream()
                        .filter(x -> ThrallUtils.isThrall(x))
                        .map(x -> manager.getThrall(x.getUniqueId()))
                        .filter(state -> state.belongsTo(player) && state.isValidEntity())
                        .forEach(state -> state.setSelected(true));
            }
        }
    }
}
