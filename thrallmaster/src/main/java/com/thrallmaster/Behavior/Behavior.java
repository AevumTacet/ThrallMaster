package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;

import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.ThrallUtils;
import com.thrallmaster.IO.Serializable;
import com.thrallmaster.States.ThrallState;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;


public abstract class Behavior implements Serializable
{
    protected UUID entityID;
    protected ThrallState state;

    public Behavior(UUID entityID, ThrallState state)
    {
        this.entityID = entityID;
        this.state = state;
    }
    public abstract String getBehaviorName();
    public abstract void onBehaviorStart();
    public abstract void onBehaviorTick();
    
    public void onBehaviorStuck() {}
    protected void onSetPersistentData(ReadWriteNBT nbt) {}
    protected void onRemovePersistentData(ReadWriteNBT nbt)  {}

    protected <T extends LivingEntity> void targetNearbyEntities(Class<T> filterClass)
    {
        Skeleton entity = this.getEntity();

        LivingEntity nearestEntity = ThrallUtils.findNearestEntity(entity, filterClass);
        if (nearestEntity != null)
        {
            state.target = nearestEntity;
            state.setBehavior(new HostileBehavior(entityID, state, this));
        } 
    }

    public void onBehaviorInteract(Material material)
    {
        Skeleton entity = this.getEntity();

        if (MaterialUtils.isAir(material))
        {
            state.setBehavior(new FollowBehavior(entityID, state));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1, 0.6f);
        }

        if (MaterialUtils.isMelee(material))
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
            
            state.getOwner().sendMessage(entity.getName() + " is now " + state.aggressionState.toString().toLowerCase());
        }
    }
    
    public final Skeleton getEntity()
    {
        return (Skeleton) Bukkit.getEntity(entityID);
    }

    @Override
    public void export(NBTCompound nbt) 
    {
        var comp = nbt.addCompound("Behavior");
        this.onSetPersistentData(comp); 
    }

}
