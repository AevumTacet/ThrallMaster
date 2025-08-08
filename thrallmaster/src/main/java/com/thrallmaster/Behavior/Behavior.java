package com.thrallmaster.Behavior;

import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractSkeleton;
import com.thrallmaster.AggressionState;
import com.thrallmaster.Main;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.ThrallManager;
import com.thrallmaster.IO.Serializable;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.ThrallUtils;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public abstract class Behavior implements Serializable {
    protected static ThrallManager manager = Main.manager;
    protected UUID entityID;
    protected ThrallState state;
    protected Random random;

    public Behavior(UUID entityID, ThrallState state) {
        this.entityID = entityID;
        this.state = state;
        this.random = new Random();
    }

    public abstract String getBehaviorName();

    public abstract void onBehaviorStart();

    public abstract void onBehaviorTick();

    public void onBehaviorStuck() {
    }

    protected void onSetPersistentData(ReadWriteNBT nbt) {
    }

    protected void onRemovePersistentData(ReadWriteNBT nbt) {
    }

    public void onBehaviorInteract(Material material) {
        AbstractSkeleton entity = this.getEntity();

        if (MaterialUtils.isMelee(material)) {
            switch (state.aggressionState) {
                case DEFENSIVE:
                    state.aggressionState = AggressionState.HOSTILE;
                    entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 0.6f);
                    break;

                case HOSTILE:
                    state.aggressionState = AggressionState.DEFENSIVE;
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 0.9f);
                    break;

                default:
                    break;
            }

            ThrallUtils.notifyOwner(state,
                    String.format(Settings.BEHAVIOR_CHANGED_MSG, entity.getName(),
                            Settings.AGGRESSION_MAP.get(state.aggressionState)));
        }
    }

    public final AbstractSkeleton getEntity() {
        return (AbstractSkeleton) Bukkit.getEntity(entityID);
    }

    @Override
    public void export(ReadWriteNBT nbt) {
        var comp = nbt.getOrCreateCompound("Behavior");
        this.onSetPersistentData(comp);
    }

}
