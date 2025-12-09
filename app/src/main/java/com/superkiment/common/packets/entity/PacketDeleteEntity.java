package com.superkiment.common.packets.entity;

import com.superkiment.common.packets.Packet;

public class PacketDeleteEntity extends Packet {
    private static final long serialVersionUID = 1L;

    public String entityId;

    public PacketDeleteEntity() {
    }

    public PacketDeleteEntity(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public PacketType getType() {
        return PacketType.DELETE_ENTITY;
    }
}