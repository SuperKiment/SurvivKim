package com.superkiment.common.entities;

import com.superkiment.common.Time;
import com.superkiment.common.collisions.Collisionable;
import com.superkiment.common.shapes.Shape;
import com.superkiment.common.collisions.CollisionShape;
import org.joml.Vector2d;

/**
 * Super-classe qui permettra de prendre des dégâts et autres effets.
 */
public class Projectile extends Entity {

    private Projectile() {
    }

    public Projectile(Vector2d pos, Vector2d trajectory) {
        super(pos);
        this.dirDepl = new Vector2d(trajectory.x, trajectory.y);
        this.dirLookTarget = new Vector2d(trajectory.x, trajectory.y);
        this.dirLookLerp = new Vector2d(trajectory.x, trajectory.y);

        this.speed = 50f;
        this.doReactCollision = false;

        this.shapeModel.addShape(
                new Shape(new Vector2d(0, 0),
                        new Vector2d(10, 10),
                        Shape.ShapeType.RECT
                )
        );
        this.collisionsManager.addCollisionShape(
                new CollisionShape(new Vector2d(0, 0),
                        new Vector2d(10, 10),
                        Shape.ShapeType.RECT_OUTLINE,
                        this
                )
        );
    }

    @Override
    protected void updateMovement() {
        dirLookTarget.set(dirDepl.x, dirDepl.y);

        Vector2d mvt = new Vector2d(dirDepl.x, dirDepl.y);
        mvt
                .normalize()
                .mul(speed)
                .mul(Time.getDeltaTime());
        pos.add(mvt);

        dirtyPosition = true;
    }

    @Override
    public void onCollision(Collisionable other) {
        System.out.println("KABOOM");
        other.hp -= 10;
        other.dirtyOtherAttribute = true;
        deleteSelf();
    }
}
