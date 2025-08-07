package com.thrallmaster.Behavior;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Player;
import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.BehaviorUtils;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class FollowBehavior extends Behavior {
    private int elapsedTicks;

    public FollowBehavior(UUID entityID, ThrallState state) {
        super(entityID, state);
    }

    @Override
    public void onBehaviorStart() {
        var entity = this.getEntity();

        if (entity != null) {
            entity.setTarget(null);
        }
    }

    @Override
    public void onBehaviorInteract(Material material) {
        AbstractSkeleton entity = this.getEntity();

        if (MaterialUtils.isAir(material)) {
            state.setBehavior(new IdleBehavior(entityID, state));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1, 0.6f);
        }

        super.onBehaviorInteract(material);
    }

    @Override
    public void onBehaviorTick() {
        Player owner = state.getOwner();
        AbstractSkeleton entity = this.getEntity();

        if (owner == null || entity == null) {
            return;
        }

        double distance = BehaviorUtils.distance(entity, owner.getLocation());
        double speed = distance < Settings.THRALL_FOLLOW_MAX / 3 ? 1.0 : Settings.RUN_SPEED_MUL;

        if (distance < Settings.THRALL_FOLLOW_MIN) {
            entity.getPathfinder().stopPathfinding();

        } else if (distance > Settings.THRALL_FOLLOW_MAX) {
            if (!MaterialUtils.isAir(owner.getLocation().getBlock().getType())) {
                entity.teleport(owner.getLocation());
                return;
            }
        } else {
            entity.lookAt(owner);
            entity.getPathfinder().moveTo(owner.getLocation(), speed);
        }

        if (elapsedTicks % 10 == 0) {
            if (state.aggressionState == AggressionState.HOSTILE) {
                BehaviorUtils.findClosestEnemy(state, this);
            } else if (state.aggressionState == AggressionState.HEALER) {
                BehaviorUtils.findClosestAlly(state, this);
            }
        }

        elapsedTicks++;
    }

    @Override
    public String getBehaviorName() {
        return Settings.FOLLOW_NAME;
    }

    @Override
    public void onBehaviorStuck() {
        AbstractSkeleton entity = this.getEntity();
        Player owner = state.getOwner();
        if (entity == null || owner == null) {
            return;
        }
        entity.teleport(owner.getLocation());
    }

    @Override
    protected void onSetPersistentData(ReadWriteNBT nbt) {
        nbt.setString("CurrentBehavior", "FOLLOW");
    }

}
