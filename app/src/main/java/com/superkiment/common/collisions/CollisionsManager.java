package com.superkiment.common.collisions;

import com.superkiment.client.graphics.Renderable;

import java.util.ArrayList;
import java.util.List;

public class CollisionsManager {
    Renderable parent;
    List<CollisionShape> collisionShapes;

    private CollisionsManager() {
    }

    public CollisionsManager(Renderable parent) {
        this.parent = parent;
        collisionShapes = new ArrayList<>();
    }

    public void addCollisionShape(CollisionShape collisionShape) {
        parent.shapeModel.addShape(collisionShape);
        collisionShapes.add(collisionShape);
    }
}
