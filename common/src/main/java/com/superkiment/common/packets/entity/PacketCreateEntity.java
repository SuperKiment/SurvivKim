package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.Packet;

/**
 * Une classe abstraite qui hérite de Packet et qui permet la création de paquets pour la création d'entités.
 */
public abstract class PacketCreateEntity extends Packet {

    public String entityId;
    public String entityName;
    public double posX;
    public double posY;
    public String[] exceptions;
    public String[] shapesTexts;

    public PacketCreateEntity(Entity entity) {
        this.entityId = entity.id;
        this.entityName = entity.name;
        this.posX = entity.pos.x;
        this.posY = entity.pos.y;

        this.exceptions = new String[entity.numberOfCollisionExceptions()];
        for (int i = 0; i < entity.numberOfCollisionExceptions(); i++) {
            exceptions[i] = ((Entity) entity.getCollisionException(i)).id;
        }

        this.shapesTexts = new String[entity.shapeModel.shapes.size()];
        for (int i = 0; i < entity.shapeModel.shapes.size(); i++) {
            shapesTexts[i] = entity.shapeModel.shapes.get(i).text;
        }
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