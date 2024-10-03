package com.piglinenslaver.Behavior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;

import com.piglinenslaver.AggressionState;
import com.piglinenslaver.ThrallState;
import com.piglinenslaver.ThrallUtils;

public class IdleBehavior extends Behavior {
    
    public Location startLocation;

    public IdleBehavior(Skeleton entity, ThrallState state) {
        super(entity, state);
        this.startLocation = entity.getLocation();
    }

    @Override
    public String getBehaviorName()
    {
        return "Idle";
    }
    
    @Override
    public void onBehaviorStart() {
        entity.setAI(true);
    }

    @Override
    public void onBehaviorTick() {
        double distance = entity.getLocation().distance(startLocation);

        if (distance > 4)
        {
            entity.getPathfinder().moveTo(startLocation, 0.7);
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
                
                state.owner.sendMessage("El Skeleton está en estado: " + state.getAggressionState());
        }
    }
    
}
