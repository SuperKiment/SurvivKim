package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.Packet;
import org.joml.Vector2d;

import java.io.Serial;

public class PacketCreateEntity extends Packet {
    @Serial
    private static final long serialVersionUID = 1L;

    public String entityId;
    public String entityName;
    public double posX;
    public double posY;

    private PacketCreateEntity() {
    }

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

    public static Packet instanciateFromEntity(Entity e) {
        return new PacketCreateEntity(e.id, e.name, e.pos);
    }
}