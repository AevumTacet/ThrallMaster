package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;

import com.thrallmaster.AggressionState;
import com.thrallmaster.ThrallState;
import com.thrallmaster.ThrallUtils;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class HostileBehavior extends Behavior {

    private long startTime;
    private Behavior prevBehavior;

    public HostileBehavior(UUID entityID, ThrallState state, Behavior prevBehavior) {
        super(entityID, state);
        this.prevBehavior = prevBehavior;
    }

    @Override
    public void onBehaviorStart() 
    {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void onBehaviorTick() {
        Skeleton entity = this.getEntity();
        if (entity == null)
        {
            return;
        }
        if (state.target != null && state.target.isValid())
        {
            entity.setTarget(state.target);
        }
        else if (state.aggressionState == AggressionState.HOSTILE) {
            LivingEntity nearestEntity = ThrallUtils.findNearestEntity(entity);
            if (nearestEntity != null)
            {
                state.target = nearestEntity;
                entity.setTarget(nearestEntity);

                entity.getWorld().spawnParticle(Particle.SMOKE, entity.getEyeLocation().add(0, 1, 0), 10, 0.1, 0.1, 0.1, 0.01);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_STRIDER_AMBIENT, 1, 0.5f);
            }
        }

        long currentTime = System.currentTimeMillis();
        if ((state.target == null || !state.target.isValid()) || (currentTime - startTime > 12 * 1000) )
        {
            returnToPreviousState();
            this.startTime = currentTime;
        }
    }

    @Override
    public String getBehaviorName() {
        return "Attacking";
    }

    @Override
    public void onBehaviorInteract(Material material) {
        Skeleton entity = this.getEntity();

        if (material == Material.AIR)
        {
            state.aggressionState = AggressionState.DEFENSIVE;
            returnToPreviousState();
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 0.6f);
        }
    }

    public void returnToPreviousState()
    {
        state.setBehavior(prevBehavior);
        state.target = null;
        this.getEntity().setTarget(null);
    }

    @Override
    protected void onSetPersistenData(ReadWriteNBT nbt) {
        if (prevBehavior != null)
        {
            prevBehavior.onSetPersistenData(nbt);
        }
    }
    
    @Override
    protected void onRemovePersistentData(ReadWriteNBT nbt) {
        if (prevBehavior != null)
        {
            prevBehavior.onRemovePersistentData(nbt);
        }
    }
}
