package com.thrallmaster.Protocols;

import java.util.ArrayList;
import org.bukkit.entity.Entity;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Serializer;

public class SelectionOutlineProtocol extends ThrallProtocol {
    private boolean enabled;
    private Entity entity;

    public SelectionOutlineProtocol(Entity entity, boolean enabled) {
        this.entity = entity;
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @SuppressWarnings("removal")
    @Override
    public PacketContainer getPacket() {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entity.getEntityId());

        ArrayList<WrappedDataValue> metadata = new ArrayList<>();

        Serializer serializer = Registry.get(Byte.class);
        byte flag = enabled ? (byte) 0x40 : (byte) 0x00;

        WrappedDataValue entityFlags = new WrappedDataValue(0, serializer, flag);
        metadata.add(entityFlags);

        packet.getDataValueCollectionModifier().write(0, metadata);
        return packet;
    }

}
