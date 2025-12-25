package com.superkiment.common.packets;

import org.joml.Vector2d;

public class PacketCreateBlock extends Packet {

    public double posX;
    public double posY;

    public PacketCreateBlock(Vector2d position) {
        super();
        this.posX = position.x;
        this.posY = position.y;
    }

    @Override
    public PacketType getType() {
        return PacketType.CREATE_BLOCK;
    }

    @Override
    public String toString() {
        return "PacketCreateBlock{pos=(" + posX + "," + posY + ")}";
    }
}