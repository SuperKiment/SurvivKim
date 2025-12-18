package com.superkiment.common.packets;

import java.io.Serial;

public class PacketPlayerJoin extends Packet {
    @Serial
    private static final long serialVersionUID = 1L;

    public String playerId;
    public String playerName;

    public PacketPlayerJoin() {}

    public PacketPlayerJoin(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    @Override
    public PacketType getType() {
        return PacketType.PLAYER_JOIN;
    }
}