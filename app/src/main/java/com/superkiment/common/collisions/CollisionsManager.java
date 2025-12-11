package com.superkiment.common.collisions;

import java.util.ArrayList;
import java.util.List;

public class CollisionsManager {
    public Collisionable parent;
    public List<CollisionShape> collisionShapes;

    private CollisionsManager() {
    }

    public CollisionsManager(Collisionable parent) {
        this.parent = parent;
        collisionShapes = new ArrayList<>();
    }

    public void addCollisionShape(CollisionShape collisionShape) {
        parent.shapeModel.addShape(collisionShape);
        collisionShapes.add(collisionShape);
    }

    public boolean isInCollisionWith(CollisionsManager other) {
        for (CollisionShape thisShape : collisionShapes) {
            for (CollisionShape otherShape : other.collisionShapes) {
                if (thisShape.isInCollisionWith(otherShape)) {
                    System.out.println("COLLISION");
                    return true;
                }
            }
        }

        return false;
    }
}
