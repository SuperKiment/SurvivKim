package com.superkiment.common.blocks;

import com.superkiment.common.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * La collection et gestion des blocks dans le monde.
 */
public class BlocksManager {
    private final List<Block> blocks;

    public BlocksManager() {
        blocks = new CopyOnWriteArrayList<>();
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public boolean addBlock(Block newBlock) {
        for (Block block : blocks) {
            if (block.isBlockOnPos(newBlock.pos)) {
                Logger.debug("Cannot place block " + (int) newBlock.pos.x + " " + (int) newBlock.pos.y);
                return false;
            }
        }

        Logger.debug("Added block to BlockManager " + (int) newBlock.pos.x + " " + (int) newBlock.pos.y);
        blocks.add(newBlock);
        return true;
    }
}
