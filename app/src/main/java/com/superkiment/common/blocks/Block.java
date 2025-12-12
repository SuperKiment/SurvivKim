package com.superkiment.common.blocks;

import com.superkiment.common.collisions.Collisionable;
import com.superkiment.client.graphics.Shape;
import com.superkiment.client.graphics.ShapeModel;
import com.superkiment.common.collisions.CollisionsManager;
import com.superkiment.common.collisions.CollisionShape;
import org.joml.Vector2d;

/**
 * Un block dans le monde, non-déplaçable.
 */
public class Block extends Collisionable {
    public static int blockSize = 50;

    public Block(int x, int y) {
        pos = new Vector2d(x, y);
        shapeModel = new ShapeModel();
        shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(blockSize, blockSize), Shape.ShapeType.RECT));

        collisionsManager = new CollisionsManager(this);
        collisionsManager.addCollisionShape(
                new CollisionShape(
                        new Vector2d(0, 0),
                        new Vector2d(blockSize, blockSize),
                        Shape.ShapeType.RECT_OUTLINE,
                        this
                )
        );
    }

    public boolean isBlockOnPos(Vector2d pos) {
        return (int) pos.x == (int) this.pos.x && (int) pos.y == (int) this.pos.y;
    }

    @Override
    public Vector2d getWorldPosition() {
        return new Vector2d(pos.x * Block.blockSize, pos.y * Block.blockSize);
    }
}
