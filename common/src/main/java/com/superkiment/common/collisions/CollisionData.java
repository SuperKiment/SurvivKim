package com.superkiment.common.collisions;

/**
 * Structure pour stocker les données de collision
 */
public class CollisionData {
    public CollisionsManager other;
    public CollisionShape thisShape;
    public CollisionShape otherShape;
    public String collisionType;

    public CollisionData(CollisionsManager other, CollisionShape thisShape,
                         CollisionShape otherShape, String collisionType) {
        this.other = other;
        this.thisShape = thisShape;
        this.otherShape = otherShape;
        this.collisionType = collisionType;
    }
}
