package com.superkiment.common.packets;

import org.joml.Vector2d;

public class PacketCreateBlock extends Packet {
    private static final long serialVersionUID = 1L;

    public double posX;
    public double posY;

    public PacketCreateBlock() {}

    public PacketCreateBlock(Vector2d position) {
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