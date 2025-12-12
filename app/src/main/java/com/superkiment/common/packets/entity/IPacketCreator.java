package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.Packet;

/**
 * Interface fonctionnelle permettant de forcer la création et l'accessibilité à une instanciation d'entité à partir du paquet lui-même.
 */
@FunctionalInterface
public interface IPacketCreator {
    Packet instantiateFromEntity(Entity e);
}
