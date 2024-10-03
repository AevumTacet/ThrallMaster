package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WitherSkeleton;
import com.piglinenslaver.ThrallState;
import com.piglinenslaver.ThrallUtils;

public class HostileBehavior extends Behavior {

    private long startTime;
    private Behavior prevBehavior;
    public HostileBehavior(WitherSkeleton entity, ThrallState state, Behavior prevBehavior) {
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

                var builder = DamageSource.builder(DamageType.CACTUS);
                builder.withCausingEntity(state.target);
                builder.withDirectEntity(state.target);
                entity.damage(0.01, builder.build());
            }
        }

        long currentTime = System.currentTimeMillis();
        if ((state.target == null || !state.target.isValid()) || (currentTime - startTime < 12 * 1000) )
        {
            System.out.println("WitherSkeleton target is " + state.target + ". Forgetting and returning to previous state.");
            state.setBehavior(prevBehavior);
            state.target = null;
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
            state.setBehavior(new FollowBehavior(entity, state));
        }
    }
    
}
