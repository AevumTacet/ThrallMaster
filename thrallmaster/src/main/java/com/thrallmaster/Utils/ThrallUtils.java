package com.thrallmaster.Utils;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

import com.thrallmaster.AggressionState;
import com.thrallmaster.Main;
import com.thrallmaster.Settings;
import com.thrallmaster.ThrallManager;
import com.thrallmaster.States.ThrallState;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public final class ThrallUtils {
    private static ThrallManager manager = Main.manager;

    private ThrallUtils() {
    }

    public static boolean isThrall(final Entity entity) {
        return (entity instanceof AbstractSkeleton) && isEntityTracked(entity);
    }

    public static boolean isEntityTracked(final Entity entity) {
        return manager.isEntityTracked(entity.getUniqueId());
    }

    public static boolean belongsTo(final Entity entity, final Entity owner) {
        if (!(owner instanceof Player) || !isThrall(entity)) {
            return false;
        }

        final ThrallState thrall = manager.getThrall(entity.getUniqueId());
        return belongsTo(thrall, owner);

    }

    public static boolean belongsTo(final ThrallState state, final Entity owner) {
        return state.getOwnerID().equals(owner.getUniqueId());
    }

    public static boolean haveSameOwner(final ThrallState state, final Entity target) {
        return manager.getThralls(state.getOwnerID())
                .anyMatch(x -> x.getEntityID().equals(target.getUniqueId()));
    }

    public static boolean isAlly(final ThrallState state, final UUID playerID) {
        final var ownerData = manager.getOwnerData(state.getOwnerID());
        if (ownerData == null) {
            return false;
        }
        return ownerData.isAlly(playerID);
    }

    public static boolean isAlly(final ThrallState state, final ThrallState target) {
        if (target == null) {
            return false;
        }
        return isAlly(state, target.getOwnerID());
    }

    public static boolean isAlly(final ThrallState state, final Entity target) {
        if (target instanceof Player) {
            final Player player = (Player) target;
            return isAlly(state, player.getUniqueId());
        }

        final var otherState = manager.getThrall(target.getUniqueId());
        return isAlly(state, otherState);
    }

    public static boolean isFriendly(final ThrallState state, final ThrallState target) {
        return state.isSameOwner(target) || isAlly(state, target);
    }

    public static boolean isFriendly(final ThrallState state, final Entity target) {
        if (target instanceof Player) {
            if (state.aggressionState == AggressionState.DEFENSIVE) {
                return true;
            }

            final Player player = (Player) target;
            if (player.getGameMode() != GameMode.SURVIVAL) // By default, do not attack players in creative/spectator
                                                           // modes
            {
                return true;
            }

            return belongsTo(state, player) || isAlly(state, player);
        }

        if (target instanceof Wolf) {
            final Wolf wolf = (Wolf) target;
            return wolf.isTamed() && isAlly(state, wolf.getOwner().getUniqueId());
        }

        return isAlly(state, target) || haveSameOwner(state, target);
    }

    public static boolean isFriendly(final Entity entity, final Entity target) {
        if (isThrall(entity)) {
            final ThrallState state = manager.getThrall(entity.getUniqueId());
            return isFriendly(state, target);
        }
        return false;
    }

    public static double distanceToOwner(final ThrallState thrall) {
        if (!thrall.isValidEntity() || thrall.getOwner() == null) {
            return 999;
        }

        return thrall.getEntity().getLocation().distance(thrall.getOwner().getLocation());
    }

    public static void notifyPlayer(Player player, String message) {
        ((Audience) player).sendActionBar(
                Component.text(message));
    }

    public static void notifyOwner(ThrallState state, String message) {
        if (state.getOwner() != null) {
            notifyPlayer(state.getOwner(), message);
        }
    }

}
