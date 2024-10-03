package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Piglin;
import com.piglinenslaver.PiglinState;
import com.piglinenslaver.PiglinUtils;

public class HostileBehavior extends Behavior {

    private long startTime;
    private Behavior prevBehavior;
    public HostileBehavior(Piglin piglin, PiglinState state, Behavior prevBehavior) {
        super(piglin, state);
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
            LivingEntity nearestEntity = PiglinUtils.findNearestEntity(piglin);
            if (nearestEntity != null)
            {
                // Hotfix: make them attack
                state.target = nearestEntity;
                piglin.setTarget(nearestEntity);

                var builder = DamageSource.builder(DamageType.CACTUS);
                builder.withCausingEntity(state.target);
                builder.withDirectEntity(state.target);
                piglin.damage(0.01, builder.build());
            }
        }

        long currentTime = System.currentTimeMillis();
        if ((state.target == null || !state.target.isValid()) || (currentTime - startTime < 12 * 1000) )
        {
            System.out.println("Piglin target is " + state.target + ". Forgetting and returning to previous state.");
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
            state.setBehavior(new FollowBehavior(piglin, state));
        }
    }
    
}
