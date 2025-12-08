package com.superkiment.common.packets;

public class PacketEntityPosition extends Packet {
    private static final long serialVersionUID = 1L;

    public String entityId;
    public double posX;
    public double posY;
    public double dirX;
    public double dirY;
    public long timestamp;

    public PacketEntityPosition() {}

    public PacketEntityPosition(String entityId, double posX, double posY,
                                double dirX, double dirY) {
        this.entityId = entityId;
        this.posX = posX;
        this.posY = posY;
        this.dirX = dirX;
        this.dirY = dirY;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.ENTITY_POSITION;
    }

    @Override
    public String toString() {
        return "PacketEntityPosition{id=" + entityId +
                ", pos=(" + posX + "," + posY + ")}";
    }
}