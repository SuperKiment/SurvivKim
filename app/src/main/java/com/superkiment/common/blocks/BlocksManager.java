package com.superkiment.common.blocks;

import org.joml.Vector2d;

import java.util.ArrayList;

public class BlocksManager {
    private final ArrayList<Block> blocks;

    BlocksManager() {
        blocks = new ArrayList<Block>();
    }

    public ArrayList<Block> getBlocks() {
        return blocks;
    }

    public boolean addBlock(Vector2d pos) {
        for (Block block : blocks) {
            if (block.isBlockOnPos(pos)) {
                System.out.println("Cannot place block " + pos);
                return false;
            }
        }

        blocks.add(new Block((int) pos.x, (int) pos.y));
        return true;
    }
}
