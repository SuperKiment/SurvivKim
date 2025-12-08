package com.superkiment.common.packets;

import java.io.Serializable;

public abstract class Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PacketType {
        CREATE_ENTITY,
        ENTITY_POSITION,
        DELETE_ENTITY,
        PLAYER_JOIN,
        PLAYER_LEAVE
    }

    public abstract PacketType getType();
}