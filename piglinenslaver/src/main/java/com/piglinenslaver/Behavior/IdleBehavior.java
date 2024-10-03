package com.piglinenslaver.Behavior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;

import com.piglinenslaver.AggressionState;
import com.piglinenslaver.PiglinState;
import com.piglinenslaver.PiglinUtils;

public class IdleBehavior extends Behavior {
    
    public Location startLocation;

    public IdleBehavior(Piglin piglin, PiglinState state) {
        super(piglin, state);
        this.startLocation = piglin.getLocation();
    }

    @Override
    public String getBehaviorName()
    {
        return "Idle";
    }
    
    @Override
    public void onBehaviorStart() {
        piglin.setAI(true);
    }

    @Override
    public void onBehaviorTick() {
        double distance = piglin.getLocation().distance(startLocation);

        if (distance > 4)
        {
            piglin.getPathfinder().moveTo(startLocation, 0.7);
        }

        if (state.getAggressionState() == AggressionState.HOSTILE)
        {
            LivingEntity nearestEntity = PiglinUtils.findNearestEntity(piglin);
            if (nearestEntity != null)
            {
                state.setBehavior(new HostileBehavior(piglin, state, this));
            } 
        }
    }

    @Override
    public void onBehaviorInteract(Material material) {
        if (material == Material.AIR)
        {
            state.setBehavior(new FollowBehavior(piglin, state));
        }

        if (material.toString().endsWith("_SWORD"))
        {
            switch (state.getAggressionState())
                {
                    case DEFENSIVE:
                    default:
                        state.setAggressionState(AggressionState.HOSTILE);
                        break;
                        
                        case HOSTILE:
                        state.setAggressionState(AggressionState.DEFENSIVE);
                        break;
                }
                
                state.owner.sendMessage("El Piglin está en estado: " + state.getAggressionState());
        }
    }
    
}
