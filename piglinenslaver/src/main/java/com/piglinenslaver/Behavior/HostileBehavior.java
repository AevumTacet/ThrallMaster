package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import com.piglinenslaver.ThrallState;
import com.piglinenslaver.ThrallUtils;

public class HostileBehavior extends Behavior {

    private long startTime;
    private Behavior prevBehavior;
    public HostileBehavior(Skeleton entity, ThrallState state, Behavior prevBehavior) {
        super(entity, state);
        this.prevBehavior = prevBehavior;
    }

    @Override
    public void onBehaviorStart() 
    {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void onBehaviorTick() {
        if (state.target == null || !state.target.isValid())
        {
            LivingEntity nearestEntity = ThrallUtils.findNearestEntity(entity);
            if (nearestEntity != null)
            {
                // Hotfix: make them attack
                state.target = nearestEntity;
                entity.setTarget(nearestEntity);

                entity.getWorld().spawnParticle(Particle.SMOKE, entity.getEyeLocation().add(0, 1, 0), 10, 0.1, 0.1, 0.1, 0.01);
                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 0.5f);
            }
        }

        long currentTime = System.currentTimeMillis();
        if ((state.target == null || !state.target.isValid()) || (currentTime - startTime > 12 * 1000) )
        {
            System.out.println("Skeleton target is " + state.target + ". Forgetting and returning to previous state.");
            returnToPreviousState();
            this.startTime = currentTime;
        }
    }

    @Override
    public String getBehaviorName() {
        return "Hostile";
    }

    @Override
    public void onBehaviorInteract(Material material) {
        if (material == Material.AIR)
        {
            returnToPreviousState();
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 0.6f);
        }
    }

    public void returnToPreviousState()
    {
        state.setBehavior(prevBehavior);
        state.target = null;
        entity.setTarget(null);
    }
    
}
