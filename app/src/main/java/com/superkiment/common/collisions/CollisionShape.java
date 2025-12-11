package com.superkiment.common.collisions;

import com.superkiment.client.graphics.Shape;
import org.joml.Vector2d;

public class CollisionShape extends Shape {
    private CollisionShape() {
    }

    public CollisionShape(Vector2d pos, Vector2d dim, ShapeType st) {
        super(pos, dim, st);
    }
}
