package com.superkiment.common.packets;

import com.superkiment.common.blocks.Block;

public class PacketCreateBlock extends Packet {

    public int posX = 0;
    public int posY = 0;
    public Block.BlockCollisionType blockCollisionType = Block.BlockCollisionType.GROUND;

    public PacketCreateBlock(Block block) {
        super();
        this.posX = (int) block.pos.x;
        this.posY = (int) block.pos.y;
        this.blockCollisionType = block.blockCollisionType;
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