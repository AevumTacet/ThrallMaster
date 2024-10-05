package com.piglinenslaver.Behavior;

import org.bukkit.Material;
import org.bukkit.entity.Skeleton;

import com.piglinenslaver.ThrallState;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;


public abstract class Behavior {
    protected Skeleton entity;
    protected ThrallState state;

    public Behavior(Skeleton entity, ThrallState state)
    {
        this.entity = entity;
        this.state = state;
    }
    public abstract String getBehaviorName();
    public abstract void onBehaviorStart();
    public abstract void onBehaviorTick();
    public abstract void onBehaviorInteract(Material material);

    protected void onSetPersistenData(ReadWriteNBT nbt)
    {
    }
    protected void onRemovePersistentData(ReadWriteNBT nbt)
    {
    }

    public final void setEntityName()
    {
        var textComponent = Component.text("Thrall [" + this.getBehaviorName() + "]");
        entity.customName(textComponent);
    }

    public final void setPersistentData()
    {
        NBT.modifyPersistentData(entity, nbt -> 
        {
            this.onSetPersistenData(nbt);
        });
    }
    public final void removePersistentData()
    {
        NBT.modifyPersistentData(entity, nbt -> 
        {
            this.onRemovePersistentData(nbt);
        });
    }
}
