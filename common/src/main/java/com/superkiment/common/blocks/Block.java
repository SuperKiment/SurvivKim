package com.superkiment.common.blocks;

import com.superkiment.common.collisions.Collisionable;
import com.superkiment.common.shapes.Shape;
import com.superkiment.common.shapes.ShapeModel;
import com.superkiment.common.collisions.CollisionsManager;
import com.superkiment.common.collisions.CollisionShape;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;

import static com.superkiment.common.utils.StringUtils.GetLastTerm;

/**
 * Un block dans le monde, non-déplaçable.
 */
public class Block extends Collisionable {
    public static enum BlockCollisionType {
        GROUND, WALL
    }

    public static int blockSize = 50;
    public BlockCollisionType blockCollisionType = BlockCollisionType.WALL;

    public Block(int x, int y, BlockCollisionType bct) {
        pos = new Vector2d(x, y);
        blockCollisionType = bct;

        shapeModel = new ShapeModel();
        collisionsManager = new CollisionsManager(this);

        switch (blockCollisionType) {
            case GROUND -> {
                Shape shape = new Shape(new Vector2d(0, 0), new Vector2d(blockSize, blockSize), Shape.ShapeType.RECT);
                shape.lineWidth = 0f;
                shape.color = new Vector3d(0, 1, 0);
                shapeModel.addShape(shape);
            }
            case WALL -> {
                shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(blockSize, blockSize), Shape.ShapeType.RECT));
                collisionsManager.addCollisionShape(
                        new CollisionShape(
                                new Vector2d(0, 0),
                                new Vector2d(blockSize, blockSize),
                                Shape.ShapeType.RECT_OUTLINE,
                                this
                        )
                );
            }
        }

        uiShapeModels = new ArrayList<>();
    }

    public boolean isBlockOnPos(Vector2d pos) {
        return (int) pos.x == (int) this.pos.x && (int) pos.y == (int) this.pos.y;
    }

    @Override
    public Vector2d getWorldPosition() {
        return new Vector2d(pos.x * Block.blockSize, pos.y * Block.blockSize);
    }

    @Override
    public void onCollision(Collisionable other) {

    }

    @Override
    public String toString() {
        return GetLastTerm(this.getClass().getName()) + "::" + pos;
    }
}
