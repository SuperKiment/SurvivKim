package com.superkiment.common.packets;

public class PacketPlayerJoin extends Packet {
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