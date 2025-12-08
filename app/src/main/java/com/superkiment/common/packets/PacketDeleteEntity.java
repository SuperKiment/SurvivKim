package com.superkiment.common.packets;

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