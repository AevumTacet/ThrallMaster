package com.thrallmaster.States;

import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Stream;

import com.thrallmaster.IO.Serializable;
import de.tr7zw.nbtapi.NBTCompound;

public class PlayerState implements Serializable
{
    private UUID playerID;
    private HashSet<ThrallState> thralls;
    private HashSet<UUID> allies;
    
    public PlayerState(UUID playerID) 
    {
        this.playerID = playerID;
        this.thralls = new HashSet<>();
        this.allies = new HashSet<>();
    }
    
    public UUID getPlayerID() {
        return playerID;
    }
    public Stream<ThrallState> getThralls()
    {
        return this.thralls.stream();
    }

    public int getCount()
    {
        return this.thralls.size();
    }

    public ThrallState getThrall(UUID entityID)
    {
        return getThralls()
            .filter(x -> x.getEntityID().equals(entityID))
            .findFirst()
            .orElse(null);
    }

    public void addThrall(ThrallState state)
    {
        this.thralls.add(state);
    }
    public boolean removeThrall(ThrallState state)
    {
        return this.thralls.remove(state);
    }


    public void addAlly(UUID playerID)
    {
        this.allies.add(playerID);
    }
    public boolean removeAlly(UUID playerID)
    {
        return this.allies.remove(playerID);
    }
    public Stream<UUID> getAllies()
    {
        return this.allies.stream();
    }
    public boolean isAlly(UUID playerID)
    {
        return allies.contains(playerID);
    }

    @Override
    public void export(NBTCompound nbt)
    {   
        nbt.setString("PlayerID", playerID.toString());

        var thrallComp = nbt.addCompound("Thralls");
        var alliesComp = nbt.addCompound("Allies");

        getThralls().forEach(state -> state.export(thrallComp));
        getAllies().forEach(ally -> alliesComp.setInteger(ally.toString(), 0));
    }
}
