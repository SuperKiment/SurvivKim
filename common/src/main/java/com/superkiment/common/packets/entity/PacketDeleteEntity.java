package com.superkiment.common.packets.entity;

import com.superkiment.common.packets.Packet;

import java.io.Serial;

public class PacketDeleteEntity extends Packet {
    @Serial
    private static final long serialVersionUID = 1L;

    public String entityId;

    public PacketDeleteEntity(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public PacketType getType() {
        return PacketType.DELETE_ENTITY;
    }
}