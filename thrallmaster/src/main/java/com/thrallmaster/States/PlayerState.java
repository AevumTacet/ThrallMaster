package com.thrallmaster.States;

import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Stream;

import com.thrallmaster.IO.Serializable;
import de.tr7zw.nbtapi.NBTCompound;

public class PlayerState implements Serializable {
    private UUID playerID;
    private HashSet<ThrallState> thralls;
    private HashSet<UUID> allies;
    private PlayerOptions playerOptions;

    public PlayerState(UUID playerID) {
        this.playerID = playerID;
        this.thralls = new HashSet<>();
        this.allies = new HashSet<>();
        this.playerOptions = new PlayerOptions(playerID);
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public Stream<ThrallState> getThralls() {
        return this.thralls.stream();
    }

    public int getCount() {
        return this.thralls.size();
    }

    public ThrallState getThrall(UUID entityID) {
        return getThralls()
                .filter(x -> x.getEntityID().equals(entityID))
                .findFirst()
                .orElse(null);
    }

    public void addThrall(ThrallState state) {
        this.thralls.add(state);
    }

    public boolean removeThrall(ThrallState state) {
        return this.thralls.remove(state);
    }

    public void addAlly(UUID playerID) {
        this.allies.add(playerID);
    }

    public boolean removeAlly(UUID playerID) {
        return this.allies.remove(playerID);
    }

    public Stream<UUID> getAllies() {
        return this.allies.stream();
    }

    public boolean isAlly(UUID playerID) {
        return this.playerID.equals(playerID) || allies.contains(playerID);
    }

    public void setConfig(String key, String value) {
        this.playerOptions.setConfig(key, value);
    }

    @Override
    public void export(NBTCompound nbt) {
        var comp = nbt.addCompound(playerID.toString());
        comp.setString("PlayerID", playerID.toString());

        var thrallComp = comp.addCompound("Thralls");
        var alliesComp = comp.addCompound("Allies");
        var settingsComp = comp.addCompound("Settings");

        getThralls().forEach(state -> state.export(thrallComp));
        getAllies().forEach(ally -> alliesComp.setInteger(ally.toString(), 0));
        playerOptions.export(settingsComp);
    }
}
