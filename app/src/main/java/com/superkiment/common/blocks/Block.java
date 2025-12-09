package com.superkiment.common.blocks;

import com.superkiment.client.graphics.Shape;
import com.superkiment.client.graphics.ShapeModel;
import org.joml.Vector2d;

public class Block {
    public Vector2d pos;
    public ShapeModel shapeModel;

    public Block(int x, int y) {
        pos = new Vector2d(x, y);
        shapeModel = new ShapeModel();
        shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(50, 50), Shape.ShapeType.RECT));
    }

    public boolean isBlockOnPos(Vector2d pos) {
        return (int) pos.x == (int) this.pos.x && (int) pos.y == (int) this.pos.y;
    }
}
