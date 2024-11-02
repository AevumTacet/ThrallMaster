package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.ThrallUtils;
import com.thrallmaster.States.ThrallState;

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
        return "Guarding";
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

        if (state.aggressionState == AggressionState.HOSTILE)
        {
            targetNearbyEntities(Enemy.class);
        }
    }

    @Override
    public void onBehaviorStuck() 
    {
        Skeleton entity = this.getEntity();

        if (entity == null)
        {
            return;
        }
        entity.teleport(startLocation);
    }

    @Override
    public void onSetPersistentData(ReadWriteNBT nbt) {
        nbt.setString("CurrentBehavior", "IDLE");

        if (startLocation != null)
        {
            nbt.setString("IdleLocationW", startLocation.getWorld().getName());
            nbt.setDouble("IdleLocationX", startLocation.getX());
            nbt.setDouble("IdleLocationY", startLocation.getY());
            nbt.setDouble("IdleLocationZ", startLocation.getZ());
        }
    }

    @Override
    public void onRemovePersistentData(ReadWriteNBT nbt) {
        nbt.removeKey("IdleLocationW");
        nbt.removeKey("IdleLocationX");
        nbt.removeKey("IdleLocationY");
        nbt.removeKey("IdleLocationZ");
    }

}
