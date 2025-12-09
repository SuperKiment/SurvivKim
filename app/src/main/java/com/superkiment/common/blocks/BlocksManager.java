package com.superkiment.common.blocks;

import org.joml.Vector2d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlocksManager {
    private final List<Block> blocks;

    public BlocksManager() {
        blocks = new CopyOnWriteArrayList<>();
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public boolean addBlock(Vector2d pos) {
        for (Block block : blocks) {
            if (block.isBlockOnPos(pos)) {
                System.out.println("Cannot place block " + (int) pos.x + " " + (int) pos.y);
                return false;
            }
        }

        System.out.println("Added block to BlockManager " + (int) pos.x + " " + (int) pos.y);
        blocks.add(new Block((int) pos.x, (int) pos.y));
        return true;
    }
}
