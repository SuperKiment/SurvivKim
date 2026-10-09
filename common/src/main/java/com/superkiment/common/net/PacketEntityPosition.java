package com.superkiment.common.net;

public class PacketEntityPosition {
    public String entityId;
    public double posX;
    public double posY;
    public double dirX;
    public double dirY;
    public long timestamp;


    public PacketEntityPosition(String entityId, double posX, double posY, double dirX, double dirY) {
        super();
        this.entityId = entityId;
        this.posX = posX;
        this.posY = posY;
        this.dirX = dirX;
        this.dirY = dirY;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "PacketEntityPosition{id=" + entityId +
                ", pos=(" + posX + "," + posY + ")}";
    }
}