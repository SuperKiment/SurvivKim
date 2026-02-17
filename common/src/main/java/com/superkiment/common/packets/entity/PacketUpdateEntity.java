package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;

/**
 * Une classe abstraite qui hérite de Packet et qui permet la création de paquets pour la création d'entités.
 */
public class PacketUpdateEntity extends PacketCreateEntity {

    public PacketUpdateEntity(Entity entity) {
        super(entity);
    }

    @Override
    public PacketType getType() {
        return PacketType.UPDATE_ENTITY;
    }

    @Override
    public String toString() {
        return "PacketUpdateEntity{id=" + entityId + ", name=" + entityName +
                ", pos=(" + posX + "," + posY + ")}";
    }
}