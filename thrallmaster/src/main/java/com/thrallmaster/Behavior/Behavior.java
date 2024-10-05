package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Skeleton;

import com.thrallmaster.ThrallManager;
import com.thrallmaster.ThrallState;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTFile;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;


public abstract class Behavior {
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

    protected void onSetPersistenData(ReadWriteNBT nbt)
    {
    }
    protected void onRemovePersistentData(ReadWriteNBT nbt)
    {
    }

    public final Skeleton getEntity()
    {
        return (Skeleton) Bukkit.getEntity(entityID);
    }

    public final void setEntityName()
    {
        if (this.getEntity() == null)
        {
            return;
        }
        var textComponent = Component.text("Thrall [" + this.getBehaviorName() + "]");
        this.getEntity().customName(textComponent);
    }

    public final void setPersistentData()
    {
        NBTCompound nbt = ThrallManager.getNBTCompound(this.entityID);
        this.onSetPersistenData(nbt);
        ThrallManager.saveNBT();
    }
    public final void removePersistentData()
    {
        NBTCompound nbt = ThrallManager.getNBTCompound(this.entityID);
        this.onRemovePersistentData(nbt);
        ThrallManager.saveNBT();
    }
}
