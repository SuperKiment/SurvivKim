package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Projectile;
import com.superkiment.common.packets.Packet;

public class PacketCreateEntityProjectile extends PacketCreateEntity {

    public double trajX;
    public double trajY;

    public PacketCreateEntityProjectile(Projectile projectile) {
        super(projectile);
        this.trajX = projectile.dirDepl.x;
        this.trajY = projectile.dirDepl.y;
    }

    @Override
    public String toString() {
        return "PacketCreateEntityProjectile{id=" + entityId + ", name=" + entityName +
                ", pos=(" + posX + "," + posY + ")" + ", dirDepl=(" + trajX + "," + trajY + ")}";
    }

    public static Packet instanciateFromEntity(Entity e) {
        return new PacketCreateEntityProjectile((Projectile) e);
    }
}