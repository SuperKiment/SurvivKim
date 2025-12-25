package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Player;
import com.superkiment.common.packets.Packet;

public class PacketCreateEntityPlayer extends PacketCreateEntity {

    public PacketCreateEntityPlayer(Player player) {
        super(player);
    }

    @Override
    public String toString() {
        return "PacketCreateEntityProjectile{id=" + entityId + ", name=" + entityName +
                ", pos=(" + posX + "," + posY + ")}";
    }

    public static Packet instanciateFromEntity(Entity e) {
        return new PacketCreateEntityPlayer((Player) e);
    }
}