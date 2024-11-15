package com.thrallmaster.Behavior;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Skeleton;
import com.thrallmaster.AggressionState;
import com.thrallmaster.States.ThrallState;
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
        Skeleton entity = this.getEntity();
        this.startTime = System.currentTimeMillis();
        
        if (entity != null)
        {
            entity.getWorld().spawnParticle(Particle.SMOKE, entity.getEyeLocation().add(0, 1, 0), 10, 0.1, 0.1, 0.1, 0.01);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_STRIDER_AMBIENT, 1, 0.5f);
        }
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

        long currentTime = System.currentTimeMillis();
        if ((state.target == null || !state.target.isValid()) || (currentTime - startTime > 30 * 1000) )
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

    @Override
    public void onBehaviorStuck() 
    {
        prevBehavior.onBehaviorStuck();
        returnToPreviousState();
    }

    public void returnToPreviousState()
    {
        state.setBehavior(prevBehavior);
        state.target = null;
        this.getEntity().setTarget(null);
    }

    @Override
    protected void onSetPersistentData(ReadWriteNBT nbt) {
        if (prevBehavior != null)
        {
            prevBehavior.onSetPersistentData(nbt);
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
