package com.thrallmaster.Behavior;

import java.util.Comparator;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.ThrallUtils;
import com.thrallmaster.States.ThrallState;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class IdleBehavior extends Behavior {

    public Location startLocation;

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
        if (entity != null) {
            entity.setAI(true);
            entity.setTarget(null);
        }
    }

    @Override
    public void onBehaviorInteract(Material material) {
        AbstractSkeleton entity = this.getEntity();

        if (MaterialUtils.isAir(material)) {
            state.setBehavior(new FollowBehavior(entityID, state));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 1, 0.6f);
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

        double distance = ThrallUtils.getPathDistance(entity, startLocation);
        if (distance > Settings.THRALL_WANDER_MAX) {
            double speed = distance < Settings.THRALL_FOLLOW_MAX / 2 ? 1.0 : Settings.RUN_SPEED_MUL;
            entity.getPathfinder().moveTo(startLocation, speed);
        }

        double distancePlayer = state.getOwner().getLocation().distance(entity.getLocation());
        if (distancePlayer < Settings.THRALL_FOLLOW_MIN / 2) {
            entity.getPathfinder().stopPathfinding();
            entity.lookAt(state.getOwner().getEyeLocation());
        }

        if (state.aggressionState == AggressionState.HOSTILE) {
            LivingEntity nearestEntity = ThrallUtils.findNearestEntities(entity, Enemy.class)
                    .filter(x -> !ThrallUtils.isFriendly(state, x))
                    .min(Comparator
                            .comparingDouble(x -> x.getLocation().distance(entity.getLocation()) + state.selectionBias))
                    .orElse(null);
            ;
            if (nearestEntity != null) {
                state.target = nearestEntity;
                state.setBehavior(new HostileBehavior(entityID, state, this));
            }
        } else if (state.aggressionState == AggressionState.HEALER) {
            LivingEntity nearestEntity = ThrallUtils.findNearestEntities(entity, AbstractSkeleton.class)
                    .filter(x -> ThrallUtils.isFriendly(state, x))
                    .filter(x -> x.getHealth() < x.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
                    .filter(x -> !(manager.getThrall(x.getUniqueId()).getBehavior() instanceof HostileBehavior))
                    .min(Comparator
                            .comparingDouble(x -> (x.getLocation().distance(entity.getLocation()) + state.selectionBias)
                                    * x.getHealth()))
                    .orElse(null);
            ;
            if (nearestEntity != null) {
                state.target = nearestEntity;
                state.setBehavior(new HealBehavior(entityID, state, this));
            }
        }
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
