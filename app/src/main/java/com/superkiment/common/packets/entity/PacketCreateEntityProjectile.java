package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Projectile;
import com.superkiment.common.packets.Packet;
import org.joml.Vector2d;

import java.io.Serial;

public class PacketCreateEntityProjectile extends PacketCreateEntity {

    public double trajX;
    public double trajY;

    public PacketCreateEntityProjectile(String entityId, String entityName, Vector2d position, Vector2d trajectory) {
        super(entityId, entityName, position);
        this.trajX = trajectory.x;
        this.trajY = trajectory.y;
    }

    @Override
    public String toString() {
        return "PacketCreateEntityProjectile{id=" + entityId + ", name=" + entityName +
                ", pos=(" + posX + "," + posY + ")" + ", dirDepl=(" + trajX + "," + trajY + ")}";
    }

    public static Packet instanciateFromEntity(Entity e) {
        Projectile p = (Projectile) e;
        return new PacketCreateEntityProjectile(e.id, e.name, e.pos, e.dirDepl);
    }
}