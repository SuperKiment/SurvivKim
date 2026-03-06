package com.superkiment.common.blocks;

import com.superkiment.common.Logger;
import org.joml.Vector2d;

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

    public boolean addBlock(Vector2d pos) {
        for (Block block : blocks) {
            if (block.isBlockOnPos(pos)) {
                Logger.log(Logger.LogLevel.DEBUG, "Cannot place block " + (int) pos.x + " " + (int) pos.y);
                return false;
            }
        }

        Logger.log(Logger.LogLevel.DEBUG, "Added block to BlockManager " + (int) pos.x + " " + (int) pos.y);
        blocks.add(new Block((int) pos.x, (int) pos.y));
        return true;
    }
}
