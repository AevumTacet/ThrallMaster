package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Player;

import com.piglinenslaver.Main;
import com.piglinenslaver.ThrallState;

public class FollowBehavior extends Behavior {

    public FollowBehavior(WitherSkeleton entity, ThrallState state) {
        super(entity, state);
    }

    @Override
    public void onBehaviorStart() {
    }

    @Override
    public void onBehaviorTick() {
        double minDistance = Main.config.getDouble("minFollowDistance", 5.0);
        double maxDistance = Main.config.getDouble("maxFollowDistance", 20.0);
        Player owner = state.owner;
        
        double distance = entity.getLocation().distance(owner.getLocation());
        double speed = distance < maxDistance / 3 ? 0.7 : 1.0;
        
        if (distance < minDistance / 2) 
        {
            entity.setAI(true);
            return;
        } 
        else if (distance > maxDistance) 
        {
            entity.teleport(owner.getLocation());
        } 
        else 
        {
            entity.lookAt(owner);
            entity.getPathfinder().moveTo(owner.getLocation(), speed);
        }
    }

    @Override
    public String getBehaviorName() {
        return "Following";
    }

    @Override
    public void onBehaviorInteract(Material material) {
        if (material == Material.AIR)
        {
            state.setBehavior(new IdleBehavior(entity, state));
        }
    }
    
}
