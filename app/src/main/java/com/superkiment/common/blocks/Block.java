package com.superkiment.common.blocks;

import com.superkiment.client.graphics.Renderable;
import com.superkiment.client.graphics.Shape;
import com.superkiment.client.graphics.ShapeModel;
import com.superkiment.common.collisions.CollisionsManager;
import com.superkiment.common.collisions.CollisionShape;
import org.joml.Vector2d;

public class Block extends Renderable {
    public static int blockSize = 50;

    public Vector2d pos;
    CollisionsManager collisionManager;

    public Block(int x, int y) {
        pos = new Vector2d(x, y);
        shapeModel = new ShapeModel();
        shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(blockSize, blockSize), Shape.ShapeType.RECT));

        collisionManager = new CollisionsManager(this);
        collisionManager.addCollisionShape(
                new CollisionShape(
                        new Vector2d(0, 0),
                        new Vector2d(blockSize, blockSize),
                        Shape.ShapeType.RECT_OUTLINE
                )
        );
    }

    public boolean isBlockOnPos(Vector2d pos) {
        return (int) pos.x == (int) this.pos.x && (int) pos.y == (int) this.pos.y;
    }
}
