package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;

import com.piglinenslaver.Main;
import com.piglinenslaver.PiglinState;

public class FollowBehavior extends Behavior {

    public FollowBehavior(Piglin piglin, PiglinState state) {
        super(piglin, state);
    }

    @Override
    public void onBehaviorStart() {
    }

    @Override
    public void onBehaviorTick() {
        double minDistance = Main.config.getDouble("minFollowDistance", 5.0);
        double maxDistance = Main.config.getDouble("maxFollowDistance", 20.0);
        Player owner = state.owner;
        
        double distance = piglin.getLocation().distance(owner.getLocation());
        double speed = distance < maxDistance / 3 ? 0.7 : 1.0;
        
        if (distance < minDistance / 2) 
        {
            piglin.setAI(true);
            return;
        } 
        else if (distance > maxDistance) 
        {
            piglin.teleport(owner.getLocation());
        } 
        else 
        {
            piglin.lookAt(owner);
            piglin.getPathfinder().moveTo(owner.getLocation(), speed);
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
            state.setBehavior(new IdleBehavior(piglin, state));
        }
    }
    
}
