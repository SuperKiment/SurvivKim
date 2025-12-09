package com.superkiment.common.entities;

import com.superkiment.client.graphics.Shape;
import org.joml.Vector2d;

public class Player extends Entity {
    public Player() {
        super();
    }

    public Player(Vector2d pos) {
        super(pos);

        shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(20, 20), Shape.ShapeType.RECT));
        shapeModel.addShape(new Shape(new Vector2d(20, 0), new Vector2d(10, 10), Shape.ShapeType.RECT));
    }
}
