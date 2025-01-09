package com.thrallmaster.Behavior;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.BehaviorUtils;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class IdleBehavior extends Behavior {
    public Location startLocation;
    private int elapsedTicks;
    private static final BlockFace[] DIRECTIONS = {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
    };

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

            entity.getPathfinder().moveTo(startLocation, 1);
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

        double distancePlayer = BehaviorUtils.distance(entity, state.getOwner().getLocation());
        if (distancePlayer < Settings.THRALL_FOLLOW_MIN / 2) {
            entity.getPathfinder().stopPathfinding();
            entity.lookAt(state.getOwner().getEyeLocation());
            return;
        }
        double distance = BehaviorUtils.distance(entity, startLocation);
        if (distance > Settings.THRALL_WANDER_MAX) {
            double speed = distance < Settings.THRALL_FOLLOW_MAX / 2 ? 1.0 : Settings.RUN_SPEED_MUL;
            entity.getPathfinder().moveTo(startLocation, speed);
            return;
        }

        if (elapsedTicks % 30 == 0) {
            Bukkit.getMobGoals().removeGoal(entity, VanillaGoal.RANDOM_STROLL);
            randomWalk(entity);
        }

        if (elapsedTicks % 4 == 0) {
            if (state.aggressionState == AggressionState.HOSTILE) {
                BehaviorUtils.findClosestEnemy(state, this);
            } else if (state.aggressionState == AggressionState.HEALER) {
                BehaviorUtils.findClosestAlly(state, this);
            }
        }

        elapsedTicks++;
    }

    private void randomWalk(AbstractSkeleton entity) {
        Block block = startLocation.getBlock();

        for (int i = 0; i < Settings.THRALL_WANDER_MAX; i++) {

            BlockFace face = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            Block relative = block.getRelative(face);

            if (relative.getType() == Material.AIR) {
                Block relativeDown = relative.getRelative(BlockFace.DOWN);
                if (relativeDown.isSolid()) {
                    block = relativeDown;
                } else {
                    break;
                }
            } else if (!relative.isSolid()) {
                break;
            }

            for (int j = 0; j < 2; j++) {
                Block relativeUp = relative.getRelative(BlockFace.UP);
                if (relativeUp.getType() == Material.AIR) {
                    block = relative;
                    break;
                }
                relative = relativeUp;
            }
        }
        entity.getPathfinder().moveTo(block.getLocation().add(0, 1, 0), 1);

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
