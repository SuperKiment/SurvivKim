package com.superkiment.common.packets;

public class PacketPlayerJoin extends Packet {

    public String playerId;
    public String playerName;

    public PacketPlayerJoin(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    @Override
    public PacketType getType() {
        return PacketType.PLAYER_JOIN;
    }
}