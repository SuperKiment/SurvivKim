package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Player;
import com.superkiment.common.packets.Packet;
import org.joml.Vector2d;

public class PacketCreateEntityPlayer extends PacketCreateEntity {

    public PacketCreateEntityPlayer(String entityId, String entityName, Vector2d position) {
        super(entityId, entityName, position);

    }

    @Override
    public String toString() {
        return "PacketCreateEntityProjectile{id=" + entityId + ", name=" + entityName +
                ", pos=(" + posX + "," + posY + ")}";
    }

    public static Packet instanciateFromEntity(Entity e) {
        Player p = (Player) e;
        return new PacketCreateEntityPlayer(e.id, e.name, e.pos);
    }
}