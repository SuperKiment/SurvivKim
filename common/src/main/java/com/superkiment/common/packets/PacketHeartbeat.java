package com.superkiment.common.packets;

public class PacketHeartbeat extends Packet {
    public String playerId;
    public long timestamp;

    public PacketHeartbeat(String playerId, long timestamp) {
        this.playerId = playerId;
        this.timestamp = timestamp;
    }

    @Override
    public PacketType getType() {
        return PacketType.HEARTBEAT;
    }

    @Override
    public String toString() {
        return "PacketHeartbeat{playerId=" + playerId + "}";
    }
}
