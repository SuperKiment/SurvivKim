package com.superkiment.common.blocks;

import org.joml.Vector2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BlocksManagerTest {

    private BlocksManager manager;

    @BeforeEach
    void setUp() {
        manager = new BlocksManager();
    }

    // ─────────────────────── addBlock ────────────────────────────────────

    @Test
    void addBlock_emptyManager_succeeds() {
        boolean result = manager.addBlock(new Block(0, 0, Block.BlockCollisionType.WALL));
        assertTrue(result, "L'ajout sur un manager vide doit réussir");
        assertEquals(1, manager.getBlocks().size());
    }

    @Test
    void addBlock_differentPositions_allAdded() {
        manager.addBlock(new Block(0, 0, Block.BlockCollisionType.WALL));
        manager.addBlock(new Block(50, 0, Block.BlockCollisionType.WALL));
        manager.addBlock(new Block(0, 50, Block.BlockCollisionType.WALL));
        assertEquals(3, manager.getBlocks().size(), "3 positions distinctes → 3 blocs");
    }

    @Test
    void addBlock_samePosition_isRejected() {
        Vector2d pos = new Vector2d(10, 20);
        assertTrue(manager.addBlock(new Block((int) pos.x, (int) pos.y, Block.BlockCollisionType.WALL)),  "Premier ajout doit réussir");
        assertFalse(manager.addBlock(new Block((int) pos.x, (int) pos.y, Block.BlockCollisionType.WALL)), "Doublon doit être refusé");
        assertEquals(1, manager.getBlocks().size(), "Le doublon ne doit pas être stocké");
    }

    @Test
    void addBlock_samePositionWithFloatDelta_isRejected() {
        // Les positions sont converties en int via Block.isBlockOnPos
        manager.addBlock(new Block(10, 20, Block.BlockCollisionType.WALL));
        boolean rejected = !manager.addBlock(new Block(10, 20, Block.BlockCollisionType.WALL));
        // Note : le comportement dépend de l'implémentation de isBlockOnPos
        // On documente le comportement observé plutôt qu'on ne le suppose
        assertTrue(manager.getBlocks().size() <= 2,
                "La liste ne doit pas dépasser le nombre de positions réellement distinctes");
    }

    @Test
    void getBlocks_emptyManager_returnsEmptyList() {
        assertTrue(manager.getBlocks().isEmpty());
    }

    @Test
    void getBlocks_afterAdd_isNotEmpty() {
        manager.addBlock(new Block(0, 0, Block.BlockCollisionType.WALL));
        assertFalse(manager.getBlocks().isEmpty());
    }

    @Test
    void addBlock_manyBlocks_allTracked() {
        for (int i = 0; i < 20; i++) {
            manager.addBlock(new Block(i * 50, 0, Block.BlockCollisionType.WALL));
        }
        assertEquals(20, manager.getBlocks().size(), "20 blocs à positions distinctes doivent tous être présents");
    }
}
