package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.thrallmaster.AggressionState;
import com.thrallmaster.Main;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.ThrallUtils;
import com.thrallmaster.States.ThrallState;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class FollowBehavior extends Behavior {

    public FollowBehavior(UUID entityID, ThrallState state) {
        super(entityID, state);
    }

    @Override
    public void onBehaviorStart() {
        var entity = this.getEntity();
        
        if (entity != null)
        {
            entity.setTarget(null);
        }
    }

    @Override
    public void onBehaviorTick() {
        double minDistance = Main.config.getDouble("minFollowDistance", 5.0);
        double maxDistance = Main.config.getDouble("maxFollowDistance", 20.0);

        Player owner = Bukkit.getPlayer(state.getOwnerID());
        Skeleton entity = this.getEntity();

        if (owner == null || entity == null)
        {
            return;
        }
        
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


        if (state.aggressionState == AggressionState.HOSTILE)
        {
            LivingEntity nearestEntity = ThrallUtils.findNearestEntity(entity);
            if (nearestEntity != null)
            {
                state.setBehavior(new HostileBehavior(entityID, state, this));
            } 
        }
    }

    @Override
    public String getBehaviorName() {
        return "Following";
    }

    @Override
    public void onBehaviorInteract(Material material) {
        Skeleton entity = this.getEntity();
        
        if (MaterialUtils.isAir(material))
        {
            state.setBehavior(new IdleBehavior(entityID, state));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1, 0.5f);
        }

        if (MaterialUtils.isSword(material))
        {
            switch (state.aggressionState)
                {
                    case DEFENSIVE:
                    default:
                        state.aggressionState = AggressionState.HOSTILE;
                        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 0.6f);
                        break;
                        
                        case HOSTILE:
                        state.aggressionState = AggressionState.DEFENSIVE;
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 0.9f);
                        break;
                }
            state.getOwner().sendMessage("El Skeleton está en estado: " + state.aggressionState);
        }
    }

    @Override
    public void onBehaviorStuck() 
    {
        Skeleton entity = this.getEntity();
        Player owner = Bukkit.getPlayer(state.getOwnerID());

        if (entity == null || owner == null)
        {
            return;
        }
        entity.teleport(owner.getLocation());
    }
    
}
