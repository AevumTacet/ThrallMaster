package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import com.thrallmaster.AggressionState;
import com.thrallmaster.ThrallState;
import com.thrallmaster.ThrallUtils;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class IdleBehavior extends Behavior {
    
    public Location startLocation;

    public IdleBehavior(UUID entityID, ThrallState state) {
        this(entityID, state, Bukkit.getEntity(entityID).getLocation());
    }

    public IdleBehavior(UUID entityID, ThrallState state, Location startLocation) {
        super(entityID, state);
        this.startLocation = startLocation;
    }

    @Override
    public String getBehaviorName()
    {
        return "Idle";
    }
    
    @Override
    public void onBehaviorStart() 
    {
        Skeleton entity = this.getEntity();
        if (entity != null)
        {
            entity.setAI(true);
            entity.setTarget(null);
        }
    }

    @Override
    public void onBehaviorTick() {
        Skeleton entity = this.getEntity();

        if (entity == null)
        {
            return;
        }

        if (startLocation == null)
        {
            System.err.println("Thrall start location was null, defaulting to current location.");
            startLocation = entity.getLocation();
        }

        double distance = entity.getLocation().distance(startLocation);
        if (distance > 4)
        {
            entity.getPathfinder().moveTo(startLocation, 1.0);
        }

        if (state.getAggressionState() == AggressionState.HOSTILE)
        {
            LivingEntity nearestEntity = ThrallUtils.findNearestEntity(entity);
            if (nearestEntity != null)
            {
                state.setBehavior(new HostileBehavior(entityID, state, this));
            } 
        }
    }

    @Override
    public void onBehaviorInteract(Material material) {
        Skeleton entity = this.getEntity();

        if (material == Material.AIR)
        {
            state.setBehavior(new FollowBehavior(entityID, state));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1, 0.6f);
        }

        if (material.toString().endsWith("_SWORD"))
        {
            switch (state.getAggressionState())
            {
                case DEFENSIVE:
                default:
                    state.setAggressionState(AggressionState.HOSTILE);
                    entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 0.6f);
                    break;
                    
                    case HOSTILE:
                    state.setAggressionState(AggressionState.DEFENSIVE);
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 0.9f);
                    break;
            }
            
            state.getOwner().sendMessage("El Skeleton está en estado: " + state.getAggressionState());
            this.setPersistentData();
        }
    }

    @Override
    public void onSetPersistenData(ReadWriteNBT nbt) {
        nbt.setString("CurrentBehavior", "IDLE");
        nbt.setString("IdleLocationW", startLocation.getWorld().getName());
        nbt.setDouble("IdleLocationX", startLocation.getX());
        nbt.setDouble("IdleLocationY", startLocation.getY());
        nbt.setDouble("IdleLocationZ", startLocation.getZ());
        nbt.setEnum("AgressionState", state.getAggressionState());
    }

    @Override
    public void onRemovePersistentData(ReadWriteNBT nbt) {
        nbt.removeKey("IdleLocationW");
        nbt.removeKey("IdleLocationX");
        nbt.removeKey("IdleLocationY");
        nbt.removeKey("IdleLocationZ");
    }

}
