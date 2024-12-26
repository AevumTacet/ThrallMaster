package com.thrallmaster.Behavior;

import java.util.Comparator;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import com.thrallmaster.AggressionState;
import com.thrallmaster.MaterialUtils;
import com.thrallmaster.Settings;
import com.thrallmaster.ThrallUtils;
import com.thrallmaster.States.ThrallState;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public class FollowBehavior extends Behavior {

    public FollowBehavior(UUID entityID, ThrallState state) {
        super(entityID, state);
    }

    @Override
    public void onBehaviorStart() {
        var entity = this.getEntity();
        entity.setAI(false);

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
        Player owner = Bukkit.getPlayer(state.getOwnerID());
        AbstractSkeleton entity = this.getEntity();

        if (owner == null || entity == null) {
            return;
        }

        double distance = entity.getLocation().distance(owner.getLocation());
        double speed = distance < Settings.THRALL_FOLLOW_MAX / 3 ? 1.0 : Settings.RUN_SPEED_MUL;

        if (distance < Settings.THRALL_FOLLOW_MIN) {
            entity.getPathfinder().stopPathfinding();
        } else if (distance > Settings.THRALL_FOLLOW_MAX) {
            if (!(owner.isFlying() || owner.isSwimming())) {
                entity.teleport(owner.getLocation());
            }
        } else {
            entity.lookAt(owner);
            entity.getPathfinder().moveTo(owner.getLocation(), speed);
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
    public String getBehaviorName() {
        return Settings.FOLLOW_NAME;
    }

    @Override
    public void onBehaviorStuck() {
        AbstractSkeleton entity = this.getEntity();
        Player owner = Bukkit.getPlayer(state.getOwnerID());

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
