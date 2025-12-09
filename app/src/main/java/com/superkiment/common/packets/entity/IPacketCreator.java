package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.Packet;

@FunctionalInterface
public interface IPacketCreator {
    Packet instanciateFromEntity(Entity e);
}
