package com.piglinenslaver.Behavior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;

import com.piglinenslaver.AggressionState;
import com.piglinenslaver.ThrallState;
import com.piglinenslaver.ThrallUtils;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class IdleBehavior extends Behavior {
    
    public Location startLocation;

    public IdleBehavior(Skeleton entity, ThrallState state) {
        this(entity, state, entity.getLocation());
    }

    public IdleBehavior(Skeleton entity, ThrallState state, Location startLocation) {
        super(entity, state);
        this.startLocation = startLocation;
    }

    @Override
    public String getBehaviorName()
    {
        return "Idle";
    }
    
    @Override
    public void onBehaviorStart() {
        entity.setAI(true);
        entity.setTarget(null);
    }

    @Override
    public void onBehaviorTick() {
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
                state.setBehavior(new HostileBehavior(entity, state, this));
            } 
        }
    }

    @Override
    public void onBehaviorInteract(Material material) {
        if (material == Material.AIR)
        {
            state.setBehavior(new FollowBehavior(entity, state));
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
            
            state.owner.sendMessage("El Skeleton está en estado: " + state.getAggressionState());
            this.setPersistentData();
        }
    }

    @Override
    public void onSetPersistenData(ReadWriteNBT nbt) {
        nbt.setString("CurrentBehavior", "IDLE");
        nbt.setIntArray("IdleLocation", new int[]{startLocation.blockX(), startLocation.blockY(), startLocation.blockZ()});
        nbt.setEnum("AgressionState", state.getAggressionState());
    }

    @Override
    public void onRemovePersistentData(ReadWriteNBT nbt) {
        nbt.removeKey("IdleLocation");
    }

}
