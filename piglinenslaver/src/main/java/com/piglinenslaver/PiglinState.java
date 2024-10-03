package com.piglinenslaver;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.piglinenslaver.Behavior.Behavior;

public class PiglinState
{
    public Player owner;
    public long lastToggleTime;
    private AggressionState m_AggressionState;
    private Behavior m_Behavior;
    
    public LivingEntity target;
    
    public PiglinState(Player owner) {
        this.owner = owner;
        this.lastToggleTime = 0;
        this.m_AggressionState = AggressionState.DEFENSIVE;
        
        this.target = null;
    }
    
    public Behavior getBehavior() {
        return m_Behavior;
    }

    public void setBehavior(Behavior m_Behavior) {
        this.m_Behavior = m_Behavior;
        this.m_Behavior.onBehaviorStart();
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

    public boolean isSameOwner(PiglinState state) {
        if (state == null)
        {
            return false;
        }
        return state != null && state.owner.equals(this.owner);
    }
}