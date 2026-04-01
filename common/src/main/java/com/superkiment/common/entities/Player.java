package com.superkiment.common.entities;

import com.superkiment.common.shapes.Shape;
import com.superkiment.common.collisions.CollisionShape;
import org.joml.Vector2d;

/**
 * Le Joueur et autres joueurs.
 */
public class Player extends Entity {

    Vector2d respawnPoint;

    public Player(Vector2d pos) {
        super(pos);

        shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(20, 20), Shape.ShapeType.RECT));
        shapeModel.addShape(new Shape(new Vector2d(20, 0), new Vector2d(10, 10), Shape.ShapeType.RECT));

        collisionsManager.addCollisionShape(
                new CollisionShape(
                        new Vector2d(0, 0),
                        new Vector2d(25, 25),
                        Shape.ShapeType.RECT_OUTLINE,
                        this
                )
        );

        respawnPoint = new Vector2d(pos.x, pos.y);
    }

    @Override
    public void deleteSelf() {
        respawn();
    }

    public void respawn() {
        pos.set(respawnPoint.x, respawnPoint.y);
        this.fullHealth();
    }
}
