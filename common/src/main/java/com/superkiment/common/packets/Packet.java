package com.superkiment.common.packets;

import java.io.Serial;
import java.io.Serializable;

public abstract class Packet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    protected Packet() {
    }

    public enum PacketType {
        CREATE_ENTITY,
        UPDATE_ENTITY,
        ENTITY_POSITION,
        DELETE_ENTITY,
        PLAYER_JOIN,
        PLAYER_LEAVE,
        CREATE_BLOCK,
        DELETE_BLOCK,
        BULK_POSITION
    }

    public abstract PacketType getType();
}