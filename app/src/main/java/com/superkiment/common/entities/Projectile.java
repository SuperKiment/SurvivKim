package com.superkiment.common.entities;

import com.superkiment.client.Time;
import com.superkiment.client.graphics.Shape;
import com.superkiment.client.graphics.ShapeModel;
import com.superkiment.common.collisions.CollisionShape;
import org.joml.Vector2d;

public class Projectile extends Entity {
    private Projectile() {
    }

    public Projectile(Vector2d pos, Vector2d trajectory) {
        super(pos);
        this.dirDepl = new Vector2d(trajectory.x, trajectory.y);
        this.dirLookTarget = new Vector2d(trajectory.x, trajectory.y);
        this.dirLookLerp = new Vector2d(trajectory.x, trajectory.y);

        this.speed = 50f;

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
}
