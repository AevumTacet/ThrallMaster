package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Skeleton;

import com.thrallmaster.AggressionState;
import com.thrallmaster.ThrallManager;
import com.thrallmaster.IO.Serializable;
import com.thrallmaster.IO.NBTExporter;
import com.thrallmaster.States.ThrallState;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


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
    public abstract void onBehaviorInteract(Material material);
    
    public void onBehaviorStuck() {}
    protected void onSetPersistenData(ReadWriteNBT nbt) {}
    protected void onRemovePersistentData(ReadWriteNBT nbt)  {}

    public final Skeleton getEntity()
    {
        return (Skeleton) Bukkit.getEntity(entityID);
    }

    @Override
    public void export(NBTCompound nbt) 
    {
        var comp = nbt.addCompound("Behavior");
        this.onSetPersistenData(comp); 
    }

}
