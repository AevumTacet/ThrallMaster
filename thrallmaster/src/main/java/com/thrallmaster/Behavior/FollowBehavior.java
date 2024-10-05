package com.thrallmaster.Behavior;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.thrallmaster.AggressionState;
import com.thrallmaster.Main;
import com.thrallmaster.ThrallState;
import com.thrallmaster.ThrallUtils;

public class FollowBehavior extends Behavior {

    public FollowBehavior(Skeleton entity, ThrallState state) {
        super(entity, state);
    }

    @Override
    public void onBehaviorStart() {
        entity.setTarget(null);
    }

    @Override
    public void onBehaviorTick() {
        double minDistance = Main.config.getDouble("minFollowDistance", 5.0);
        double maxDistance = Main.config.getDouble("maxFollowDistance", 20.0);
        Player owner = Bukkit.getPlayer(state.ownerID);
        
        double distance = entity.getLocation().distance(owner.getLocation());
        double speed = distance < maxDistance / 3 ? 1.0 : 1.5;
        
        if (distance < minDistance / 2) 
        {
            entity.getPathfinder().moveTo(owner.getLocation(), 0);
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
    public String getBehaviorName() {
        return "Following";
    }

    @Override
    public void onBehaviorInteract(Material material) {
        if (material == Material.AIR)
        {
            state.setBehavior(new IdleBehavior(entity, state));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1, 0.5f);
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
            state.getOwner().sendMessage("El Skeleton está en estado: " + state.getAggressionState());
        }
    }
    
}
