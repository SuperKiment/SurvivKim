package com.superkiment.common.packets;

import org.joml.Vector2d;

public class PacketCreateEntity extends Packet {
    private static final long serialVersionUID = 1L;

    public String entityId;
    public String entityName;
    public double posX;
    public double posY;

    public PacketCreateEntity() {}

    public PacketCreateEntity(String entityId, String entityName, Vector2d position) {
        this.entityId = entityId;
        this.entityName = entityName;
        this.posX = position.x;
        this.posY = position.y;
    }

    @Override
    public PacketType getType() {
        return PacketType.CREATE_ENTITY;
    }

    @Override
    public String toString() {
        return "PacketCreateEntity{id=" + entityId + ", name=" + entityName +
                ", pos=(" + posX + "," + posY + ")}";
    }
}