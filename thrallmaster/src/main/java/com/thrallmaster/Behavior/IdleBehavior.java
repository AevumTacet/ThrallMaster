package com.thrallmaster.Behavior;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.BehaviorUtils;
import com.thrallmaster.Utils.ThrallUtils;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class IdleBehavior extends Behavior {
    public Location startLocation;
    private int elapsedTicks;

    public IdleBehavior(UUID entityID, ThrallState state) {
        this(entityID, state, Bukkit.getEntity(entityID).getLocation());
    }

    public IdleBehavior(UUID entityID, ThrallState state, Location startLocation) {
        super(entityID, state);
        this.startLocation = startLocation;
    }

    @Override
    public String getBehaviorName() {
        return Settings.IDLE_NAME;
    }

    @Override
    public void onBehaviorStart() {
        AbstractSkeleton entity = this.getEntity();
        if (entity == null) {
            return;
        }
        entity.setAI(true);
        entity.setTarget(null);

        double distance = BehaviorUtils.distance(entity, startLocation);
        double speed = distance < Settings.THRALL_FOLLOW_MAX / 2 ? 1.0 : Settings.RUN_SPEED_MUL;
        entity.getPathfinder().moveTo(startLocation, speed);
    }

    @Override
    public void onBehaviorInteract(Material material) {
        AbstractSkeleton entity = this.getEntity();

        if (MaterialUtils.isAir(material)) {
            state.setBehavior(new FollowBehavior(entityID, state));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1, 0.6f);

            ThrallUtils.notifyOwner(state,
                    String.format(Settings.BEHAVIOR_CHANGED_MSG, entity.getName(),
                            Settings.FOLLOW_VERB));
        }

        super.onBehaviorInteract(material);
    }

    @Override
    public void onBehaviorTick() {
        AbstractSkeleton entity = this.getEntity();

        if (entity == null) {
            return;
        }

        if (startLocation == null) {
            System.err.println("Thrall start location was null, defaulting to current location.");
            startLocation = entity.getLocation();
        }

        if (elapsedTicks % 10 == 0) {
            if (state.aggressionState == AggressionState.HOSTILE) {
                BehaviorUtils.findClosestEnemy(state, this);
            } else if (state.aggressionState == AggressionState.HEALER) {
                BehaviorUtils.findClosestAlly(state, this);
            }
        }

        Player owner = state.getOwner();
        if (owner != null) {
            double distancePlayer = BehaviorUtils.distance(entity, owner.getLocation());
            if (distancePlayer < Settings.THRALL_FOLLOW_MIN / 2) {
                entity.getPathfinder().stopPathfinding();
                entity.lookAt(state.getOwner().getEyeLocation());
                return;
            }
        }

        if (elapsedTicks % 100 == 0) {
            double distance = BehaviorUtils.distance(entity, startLocation);
            if (distance > Settings.THRALL_WANDER_MAX * 2) {
                double speed = distance < Settings.THRALL_FOLLOW_MAX / 2 ? 1.0 : Settings.RUN_SPEED_MUL;
                entity.getPathfinder().moveTo(startLocation, speed);
                return;
            }
            BehaviorUtils.randomWalk(entity, startLocation);
        }

        elapsedTicks++;
    }

    @Override
    public void onBehaviorStuck() {
        AbstractSkeleton entity = this.getEntity();

        if (entity == null) {
            return;
        }
        entity.teleport(startLocation);
    }

    @Override
    public void onSetPersistentData(ReadWriteNBT nbt) {
        nbt.setString("CurrentBehavior", "IDLE");

        if (startLocation != null) {
            nbt.setString("IdleLocationW", startLocation.getWorld().getName());
            nbt.setDouble("IdleLocationX", startLocation.getX());
            nbt.setDouble("IdleLocationY", startLocation.getY());
            nbt.setDouble("IdleLocationZ", startLocation.getZ());
        }
    }

    @Override
    public void onRemovePersistentData(ReadWriteNBT nbt) {
        nbt.removeKey("IdleLocationW");
        nbt.removeKey("IdleLocationX");
        nbt.removeKey("IdleLocationY");
        nbt.removeKey("IdleLocationZ");
    }

}
