package com.thrallmaster.States;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

import com.thrallmaster.AggressionState;
import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.HostileBehavior;
import com.thrallmaster.IO.Serializable;
import com.thrallmaster.Protocols.SelectionOutlineProtocol;

import de.tr7zw.nbtapi.NBTCompound;

public class ThrallState implements Serializable
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

    public void setSelected(boolean selected)
    {
        Entity entity = getEntity();
        if (entity != null && (this.selected != selected))
        {
            if (selected)
            {
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 1);
                entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, entity.getLocation().add(0, 1, 0), 10, 0.1, 0.1, 0.1, 0.05);
            } else {
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 1, 0.8f);
            }
            
            try
             {
                SelectionOutlineProtocol outline = new SelectionOutlineProtocol(entity, selected);
                outline.sendPacket(this.getOwner());
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        this.selected = selected;
        this.lastSelectionTime = System.currentTimeMillis();
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
       
        this.behavior = m_Behavior;
        this.behavior.onBehaviorStart();
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

    public boolean isValidEntity()
    {
        Entity entity = getEntity();
        return entity != null && entity.isValid();
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

    public boolean belongsTo(Entity owner) 
    {
        return ownerID.equals(owner.getUniqueId());
    }

    @Override
    public void export(NBTCompound nbt) 
    {
        var comp = nbt.addCompound(entityID.toString());
        
        comp.setString("EntityID", entityID.toString());
        comp.setString("OwnerID", ownerID.toString());
        comp.setString("AggressionState", aggressionState.toString());
        
        behavior.export(comp);
    }
}