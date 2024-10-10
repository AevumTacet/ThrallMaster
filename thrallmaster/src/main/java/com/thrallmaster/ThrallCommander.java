package com.thrallmaster;

import java.util.UUID;
import java.util.stream.Stream;

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

public class ThrallCommander 
{
    private static ThrallManager manager = Main.manager;
    
    public static void ToggleSelection(Player player)
    {
        UUID playerID = player.getUniqueId();
        RayTraceResult rayTraceResult = player.rayTraceEntities(40);
            if (rayTraceResult != null)
            {
                Entity entity = rayTraceResult.getHitEntity();
                ThrallState state = manager.getThrall(player, entity.getUniqueId());

                if (entity != null && state != null)
                {
                    state.setSelected(!state.isSelected());
                }
            }
            else{
                manager.getThralls(playerID).forEach(x -> x.setSelected(false));
            }
    }

    public static void CommandSelection(Player player)
    {
        UUID playerID = player.getUniqueId();
        Location eyeLocation = player.getEyeLocation();
        RayTraceResult rayTraceResult = player.getWorld()
            .rayTrace(eyeLocation, eyeLocation.getDirection(), 100, FluidCollisionMode.ALWAYS, true, 1.0, e -> 
            {
                return (e instanceof LivingEntity) && (e != player) && !manager.isThrall(e);
            });

        if (rayTraceResult != null)
        {
            Block block = rayTraceResult.getHitBlock();
            Entity entity = rayTraceResult.getHitEntity();
            
            if (entity != null)
            {
                manager.getThralls(playerID).filter(x -> x.isSelected() && x.isValidEntity()).forEach(state -> 
                {
                    Behavior oldBehavior = state.getBehavior();
                    state.setAttackMode(entity);
                    state.setBehavior(new HostileBehavior(state.getEntityID(), state, oldBehavior));
                    player.getWorld().playSound(state.getEntity().getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                    
                });

                player.getWorld().spawnParticle(Particle.CRIT, entity.getLocation(), 20, 0.1, 0.1, 0.1, 0.01);
                return;
            }

            else if (block != null)
            {
                manager.getThralls(playerID).filter(x -> x.isSelected() && x.isValidEntity()).forEach(state -> 
                {
                    state.setBehavior(new IdleBehavior(state.getEntityID(), state, block.getLocation()));
                    player.getWorld().playSound(state.getEntity().getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                });

                player.getWorld().spawnParticle(Particle.CRIT, block.getLocation(), 20, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    public static void HornCommand(Player player)
    {
        UUID playerID = player.getUniqueId();
        ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
        if (meta instanceof MusicInstrumentMeta)
        {
            MusicInstrumentMeta instrumentMeta = (MusicInstrumentMeta) meta;
            MusicInstrument instrument = instrumentMeta.getInstrument();
            Stream<ThrallState> thrallStream = manager.getThralls(playerID).filter(x -> x.isSelected() && x.isValidEntity());
            
            if (instrument == MusicInstrument.CALL_GOAT_HORN)
            {
                if (thrallStream.allMatch(state -> {
                    return state.getBehavior() instanceof FollowBehavior;}))
                {
                    thrallStream.forEach(state -> state.setBehavior(new IdleBehavior(state.getEntityID(), state)) );
                }
                else
                {
                    thrallStream.forEach(state -> state.setBehavior(new FollowBehavior(state.getEntityID(), state)) );
                }
            }
            else if (instrument == MusicInstrument.SEEK_GOAT_HORN)
            {
                if (thrallStream.allMatch(state -> state.aggressionState == AggressionState.HOSTILE))
                {
                    thrallStream.forEach(state -> state.aggressionState = AggressionState.DEFENSIVE);
                }
                else
                {
                    thrallStream.forEach(state -> state.aggressionState = AggressionState.HOSTILE);
                }
            }
        }
        else
        {
            ThrallManager.logger.info("ItemMeta was not MusicIntrumentMeta:(");
        }
    }

    public static void MultiSelect(Player player)
    {
        RayTraceResult rayTraceResult = player.rayTraceBlocks(40);

        if (rayTraceResult != null)
        {
            Block block = rayTraceResult.getHitBlock();
            
            if (block != null)
            {
                double selectRadius = 10;
                player.getWorld().getNearbyEntities(block.getLocation(), selectRadius, selectRadius, selectRadius).stream()
                    .filter(x -> manager.isThrall(x))
                    .map(x -> manager.getThrall(x.getUniqueId()))
                    .filter(state ->  manager.belongsTo(state, player) && state.isValidEntity())
                    .forEach(state -> state.setSelected(true));
            }
        }
    }
}
