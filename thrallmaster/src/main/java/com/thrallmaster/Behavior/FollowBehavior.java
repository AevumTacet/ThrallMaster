package com.thrallmaster.Behavior;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.BehaviorUtils;
import com.thrallmaster.Utils.ThrallUtils;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class FollowBehavior extends Behavior {
    private Entity target;

    public Entity getTarget() {
        return target;
    }

    private UUID targetID;
    private int elapsedTicks;
    private boolean notFollowingPlayer;

    public FollowBehavior(UUID entityID, ThrallState state, @NotNull Entity target) {
        super(entityID, state);
        if (target != null) {
            this.target = target;
        } else {
            this.target = state.getOwner();
        }

        this.targetID = this.target.getUniqueId();
        this.notFollowingPlayer = target != state.getOwner();
    }

    public FollowBehavior(UUID entityID, ThrallState state) {
        this(entityID, state, state.getOwner());
    }

    @Override
    public void onBehaviorStart() {
        var entity = this.getEntity();

        if (entity != null) {
            entity.setTarget(null);
        }
        if (notFollowingPlayer && ThrallUtils.isThrall(target)) {
            ThrallState leader = ThrallUtils.getThrall(target.getUniqueId());
            leader.addFollower(this.state);
        }
    }

    @Override
    public void onBehaviorInteract(Material material) {
        AbstractSkeleton entity = this.getEntity();

        if (MaterialUtils.isAir(material)) {
            state.setBehavior(new IdleBehavior(entityID, state));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1, 0.6f);

            ThrallUtils.notifyOwner(state,
                    String.format(Settings.BEHAVIOR_CHANGED_MSG, entity.getName(),
                            Settings.IDLE_VERB));
        }

        super.onBehaviorInteract(material);
    }

    @Override
    public void onBehaviorTick() {
        AbstractSkeleton entity = this.getEntity();

        if (target == null || entity == null) {
            return;
        }

        double distance = BehaviorUtils.distance(entity, target.getLocation());
        double speed = distance < Settings.THRALL_FOLLOW_MAX / 3 ? 1.0 : Settings.RUN_SPEED_MUL;

        if (distance < Settings.THRALL_FOLLOW_MIN) {
            entity.getPathfinder().stopPathfinding();

        } else if (distance > Settings.THRALL_FOLLOW_MAX) {
            if (!MaterialUtils.isAir(target.getLocation().getBlock().getType())) {
                entity.teleport(target.getLocation());
                return;
            }
        } else {
            entity.lookAt(target);
            entity.getPathfinder().moveTo(target.getLocation(), speed);
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
    public void onBehaviorEnd() {
        if (notFollowingPlayer && ThrallUtils.isThrall(target)) {
            ThrallState leader = ThrallUtils.getThrall(target.getUniqueId());
            leader.removeFollower(this.state);
        }
    }

    @Override
    public String getBehaviorName() {
        if (target != null && notFollowingPlayer) {
            return Settings.FOLLOW_NAME + " " + target.getName();
        }

        return Settings.FOLLOW_NAME;
    }

    @Override
    public void onBehaviorStuck() {
        AbstractSkeleton entity = this.getEntity();
        if (entity == null || target == null) {
            return;
        }
        entity.teleport(target.getLocation());
    }

    @Override
    protected void onSetPersistentData(ReadWriteNBT nbt) {
        nbt.setString("CurrentBehavior", "FOLLOW");
        if (notFollowingPlayer) {
            nbt.setString("FollowTarget", targetID.toString());
        }
    }

}
