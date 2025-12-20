package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.Packet;

import java.io.Serial;

/**
 * Une classe abstraite qui hérite de Packet et qui permet la création de paquets pour la création d'entités.
 */
public abstract class PacketCreateEntity extends Packet {
    @Serial
    private static final long serialVersionUID = 1L;

    public String entityId;
    public String entityName;
    public double posX;
    public double posY;

    private PacketCreateEntity() {
    }

    public PacketCreateEntity(Entity entity) {
        this.entityId = entity.id;
        this.entityName = entity.name;
        this.posX = entity.pos.x;
        this.posY = entity.pos.y;
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