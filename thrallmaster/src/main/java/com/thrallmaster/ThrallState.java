package com.thrallmaster;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.UUID;
import com.thrallmaster.Behavior.Behavior;

public class ThrallState
{
    public UUID ownerID;
    private Behavior m_Behavior;
    private AggressionState m_AggressionState;
    public LivingEntity target;
    public long lastToggleTime;
    
    public ThrallState (Player owner)
    {
        this(owner.getUniqueId());
    }

    public ThrallState(UUID ownerID) {
        this.ownerID = ownerID;
        this.lastToggleTime = 0;
        this.m_AggressionState = AggressionState.DEFENSIVE;
        this.target = null;
    }

    public Behavior getBehavior() {
        return m_Behavior;
    }

    public void setBehavior(Behavior m_Behavior) {
        if (this.m_Behavior != null)
        {
            this.m_Behavior.removePersistentData();
        }

        this.m_Behavior = m_Behavior;
        this.m_Behavior.onBehaviorStart();
        this.m_Behavior.setEntityName();
        this.m_Behavior.setPersistentData();
    }
    
    public AggressionState getAggressionState() 
    {
        return this.m_AggressionState;
    }
    public void setAggressionState(AggressionState state) {
        this.m_AggressionState = state;
    }

    public void setAttackMode(Entity target)
    {
        if (target == null || !target.isValid())
        {
            this.target = null;
        }
        
        else if (target instanceof LivingEntity){
            this.target = (LivingEntity) target;
        }
    }

    public Player getOwner()
    {
        return Bukkit.getPlayer(this.ownerID);
    }

    public boolean isSameOwner(ThrallState state) {
        if (state == null)
        {
            return false;
        }
        return state != null && state.ownerID.equals(this.ownerID);
    }
}