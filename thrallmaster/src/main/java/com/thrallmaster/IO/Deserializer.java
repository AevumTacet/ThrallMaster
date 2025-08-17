package com.thrallmaster.IO;

import java.util.UUID;
import org.bukkit.Location;
import com.thrallmaster.AggressionState;
import com.thrallmaster.Main;
import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.FollowBehavior;
import com.thrallmaster.Behavior.IdleBehavior;
import com.thrallmaster.Behavior.PatrolBehavior;
import com.thrallmaster.States.PlayerState;
import com.thrallmaster.States.ThrallState;
import com.thrallmaster.Utils.IOUtils;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;

public interface Deserializer {
    public static PlayerState readPlayerState(ReadWriteNBT nbt) {
        UUID playerID = UUID.fromString(nbt.getString("PlayerID"));
        ReadWriteNBT thralls = nbt.getCompound("Thralls");
        ReadWriteNBT allies = nbt.getCompound("Allies");
        ReadWriteNBT settings = nbt.getCompound("Settings");

        PlayerState state = new PlayerState(playerID);

        if (thralls != null) {
            thralls.getKeys().stream()
                    .map(key -> thralls.getCompound(key))
                    .map(comp -> readThrallState(comp))
                    .forEach(thrall -> state.addThrall(thrall));
        }

        if (allies != null) {
            allies.getKeys().stream()
                    .map(key -> UUID.fromString(key))
                    .forEach(id -> state.addAlly(id));
        }

        if (settings != null) {
            settings.getKeys().stream()
                    .forEach(key -> state.setConfig(key, settings.getString(key)));
        }

        return state;
    }

    public static ThrallState readThrallState(ReadWriteNBT nbt) {
        UUID entityID = UUID.fromString(nbt.getString("EntityID"));
        UUID ownerID = UUID.fromString(nbt.getString("OwnerID"));
        AggressionState aggressionState = AggressionState.valueOf(nbt.getString("AggressionState"));

        ThrallState state = new ThrallState(entityID, ownerID);
        state.aggressionState = aggressionState;

        Behavior behavior = readBehavior(nbt, state);
        state.setBehavior(behavior);

        return state;
    }

    public static Behavior readBehavior(ReadWriteNBT nbt, ThrallState state) {
        ReadWriteNBT comp = nbt.getCompound("Behavior");
        if (comp == null) {
            Main.plugin.getLogger().warning("Thrall behavior block is undefined");
            return new FollowBehavior(state.getEntityID(), state);
        }

        String mode = comp.getString("CurrentBehavior");
        Behavior behavior = switch (mode) {
            case "IDLE":
                if (comp.hasTag("IdleLocation")) {
                    Location startLocation = IOUtils.readLocation(comp.getString("IdleLocationW"));
                    yield new IdleBehavior(state.getEntityID(), state, startLocation);
                } else {
                    Main.plugin.getLogger().warning("Warning: Idle state with no IdleLocation tag found.");
                    yield new IdleBehavior(state.getEntityID(), state, null);
                }

            case "PATROL":
                if (comp.hasTag("StartLocation") && comp.hasTag("EndLocation") && comp.hasTag("TargetIndex")) {

                    int index = comp.getInteger("TargetIndex");
                    Location startLocation = IOUtils.readLocation(comp.getString("StartLocation"));
                    Location endLocation = IOUtils.readLocation(comp.getString("EndLocation"));

                    var new_behavior = new PatrolBehavior(state.getEntityID(), state, startLocation, endLocation);
                    new_behavior.setTargetIndex(index);
                    yield new_behavior;
                } else {
                    Main.plugin.getLogger()
                            .warning("Warning: Patrol state with no StartLocation or EndLocation tag found.");
                    yield new IdleBehavior(state.getEntityID(), state, null);
                }

            case "FOLLOW":
                yield new FollowBehavior(state.getEntityID(), state, UUID.fromString(comp.getString("FollowTarget")));

            default:
                Main.plugin.getLogger().warning("Thrall state is unspecified, defaulting to follow");
                yield new FollowBehavior(state.getEntityID(), state);
        };

        return behavior;
    }
}
