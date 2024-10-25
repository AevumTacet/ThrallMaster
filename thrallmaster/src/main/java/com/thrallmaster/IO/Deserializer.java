package com.thrallmaster.IO;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.thrallmaster.AggressionState;
import com.thrallmaster.Main;
import com.thrallmaster.Behavior.Behavior;
import com.thrallmaster.Behavior.FollowBehavior;
import com.thrallmaster.Behavior.IdleBehavior;
import com.thrallmaster.States.PlayerState;
import com.thrallmaster.States.ThrallState;

import de.tr7zw.nbtapi.NBTCompound;

public interface Deserializer 
{
    public static PlayerState readPlayerState(NBTCompound nbt)
    {
        UUID playerID = UUID.fromString(nbt.getString("PlayerID"));
        NBTCompound thralls = nbt.getCompound("Thralls");
        NBTCompound allies = nbt.getCompound("Allies");

        PlayerState state = new PlayerState(playerID);
        
        thralls.getKeys().stream()
            .map(key -> thralls.getCompound(key))
            .map(comp -> readThrallState(comp))
            .forEach(thrall -> state.addThrall(thrall));

        allies.getKeys().stream()
            .map(key -> UUID.fromString(key))
            .forEach(id -> state.addAlly(id));

        return state;
    }

    public static ThrallState readThrallState(NBTCompound nbt)
    {
        UUID entityID = UUID.fromString(nbt.getString("EntityID"));
        UUID ownerID = UUID.fromString(nbt.getString("OwnerID"));
        AggressionState aggressionState = AggressionState.valueOf(nbt.getString("AggressionState"));

        ThrallState state = new ThrallState(entityID, ownerID);
        state.aggressionState = aggressionState;

        Behavior behavior = readBehavior(nbt, state);
        state.setBehavior(behavior);

        return state;
    }

    public static Behavior readBehavior(NBTCompound nbt, ThrallState state)
    {
        NBTCompound comp = nbt.getCompound("Behavior");
        String mode = comp.getString("CurrentBehavior");
        
        Behavior behavior = switch(mode)
        {
            case "IDLE":
            if (comp.hasTag("IdleLocationW") && comp.hasTag("IdleLocationX") && 
                comp.hasTag("IdleLocationY") && comp.hasTag("IdleLocationZ"))
            {
                String locationW = comp.getString("IdleLocationW");
                double locationX = comp.getDouble("IdleLocationX");
                double locationY = comp.getDouble("IdleLocationY");
                double locationZ = comp.getDouble("IdleLocationZ");
                Location startLocation = new Location(Bukkit.getWorld(locationW), locationX, locationY, locationZ);
                yield new IdleBehavior(state.getEntityID(), state, startLocation);
            }
            else
            {
                Main.plugin.getLogger().warning("Warning: Idle state with no IdleLocation tag found.");
                yield new IdleBehavior(state.getEntityID(), state, null);
            }

            case "FOLLOW":
                yield new FollowBehavior(state.getEntityID(), state);
            
            default:
                Main.plugin.getLogger().warning("Thrall state is unspecified, defaulting to follow");
                yield new FollowBehavior(state.getEntityID(), state);
        };

        return behavior;
    }
}
