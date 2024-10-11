package com.thrallmaster.Behavior;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Skeleton;

import com.thrallmaster.AggressionState;
import com.thrallmaster.ThrallManager;
import com.thrallmaster.ThrallState;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


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
    
    public void onBehaviorStuck() {}
    protected void onSetPersistenData(ReadWriteNBT nbt) {}
    protected void onRemovePersistentData(ReadWriteNBT nbt)  {}

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
        
        var textComponent = Component.text("Thrall [")
                            .color(state.aggressionState == AggressionState.HOSTILE ? NamedTextColor.RED : NamedTextColor.GREEN)
                            .append(Component.text(this.getBehaviorName()))
                            .color(NamedTextColor.WHITE)
                            .append(Component.text("]"));
        if (this.state.isSelected())
        {
            textComponent = textComponent.append(Component.text("[S]"));
        }
        this.getEntity().customName(textComponent);
    }

    public final void setPersistentData()
    {
        NBTCompound nbt = ThrallManager.getNBTCompound(entityID);

        if (nbt != null)
        {
            this.onSetPersistenData(nbt);
        }
    }
    public final void removePersistentData()
    {
        NBTCompound nbt = ThrallManager.getNBTCompound(entityID);

        if (nbt != null)
        {
            this.onRemovePersistentData(nbt);
        }
    }
}
