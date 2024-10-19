package com.thrallmaster.Protocols;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

public abstract class ThrallProtocol {
    protected static ProtocolManager protocolManager;

    public static void onLoad(Plugin plugin)
    {
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    protected abstract PacketContainer getPacket();

    public void sendPacket(Player player)
    {   
        PacketContainer packet = getPacket();
        protocolManager.sendServerPacket(player, packet);
    }
}
