package com.thrallmaster;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;

import java.util.UUID;
import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.HostileBehavior;

public class ThrallState
{
    private UUID entityID;
    public UUID getEntityID() {
        return entityID;
    }

    private UUID ownerID;
    public UUID getOwnerID() {
        return ownerID;
    }
    private Behavior behavior;
    public Behavior getBehavior() {
        return behavior;
    }
    
    private boolean selected;
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        this.lastSelectionTime = System.currentTimeMillis();

        if (behavior != null)
        {
            behavior.setEntityName();
        }

        Entity entity = getEntity();
        if (entity != null)
        {
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.75f, 0.5f);
            entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, 0.2, 0), 10, 0.1, 0.1, 0.1, 0.01);
        }
    }
    
    private long lastSelectionTime;
    public long getLastSelectionTime() {
        return lastSelectionTime;
    }
    public AggressionState aggressionState;
    public LivingEntity target;
    
    private long lastInteractionTime;

    public ThrallState (LivingEntity entity, Player owner)
    {
        this(entity.getUniqueId(), owner.getUniqueId());
    }
    
    public ThrallState(UUID entityID, UUID ownerID) {
        this.entityID = entityID;
        this.ownerID = ownerID;
        this.lastInteractionTime = 0;
        this.lastSelectionTime = 0;
        this.aggressionState = AggressionState.DEFENSIVE;
        this.target = null;
    }
    
    @Override
    public String toString() {
        return "state: {" + entityID.toString() +", "+ ownerID.toString() + "} ";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityID == null) ? 0 : entityID.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
            if (getClass() != obj.getClass())
            return false;
        ThrallState other = (ThrallState) obj;
        if (entityID == null) {
            if (other.entityID != null)
                return false;
            } else if (!entityID.equals(other.entityID))
            return false;
            return true;
        }
    
        
    public void setBehavior(Behavior m_Behavior) {
        if (this.behavior != null)
        {
            this.behavior.removePersistentData();
        }
        
        this.behavior = m_Behavior;
        this.behavior.onBehaviorStart();
        this.behavior.setEntityName();
        this.behavior.setPersistentData();
    }

    public void setAttackMode(Entity target)
    {
        if (target == null || !target.isValid())
        {
            this.target = null;
        }
        
        else if (target instanceof LivingEntity){
            this.target = (LivingEntity) target;
            setBehavior(new HostileBehavior(entityID, this, behavior));
        }
    }

    public Player getOwner()
    {
        return Bukkit.getPlayer(this.ownerID);
    }
    
    public Entity getEntity()
    {
        return Bukkit.getEntity(this.entityID);
    }

    public boolean isSameOwner(ThrallState state) {
        if (state == null)
        {
            return false;
        }
        return state != null && state.ownerID.equals(this.ownerID);
    }

    public boolean canInteract()
    {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastInteractionTime < 500)
        {
            return false;
        }

        this.lastInteractionTime = currentTime;
        return true;
    }
}