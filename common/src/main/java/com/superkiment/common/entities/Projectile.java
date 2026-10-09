package com.superkiment.common.entities;

import com.superkiment.common.Logger;
import com.superkiment.common.Time;
import com.superkiment.common.collisions.Collisionable;
import com.superkiment.common.shapes.Shape;
import com.superkiment.common.collisions.CollisionShape;
import com.superkiment.common.net.Sync;
import org.joml.Vector2d;

/**
 * Super-classe qui permettra de prendre des dégâts et autres effets.
 */
public class Projectile extends Entity {

    /**
     * L'entité qui a tiré : jamais touchée par son propre projectile.
     */
    @Sync
    public String ownerId;

    @Sync
    public Vector2d trajectory;

    private Projectile() {
        this(new Vector2d(), new Vector2d(1, 0));
    }

    public Projectile(Vector2d pos, Vector2d trajectory) {
        super(pos);
        this.trajectory = new Vector2d(trajectory);
        this.dirLookTarget.set(trajectory);
        this.dirLookLerp.set(trajectory);

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
        dirLookTarget.set(trajectory);

        Vector2d mvt = new Vector2d(trajectory);
        mvt
                .normalize()
                .mul(speed)
                .mul(Time.GetDeltaFrameTime());
        pos.add(mvt);

        dirtyPosition = true;
    }

    @Override
    public boolean hasCollisionException(Collisionable c) {
        return c instanceof Entity e && e.id.equals(ownerId) || super.hasCollisionException(c);
    }

    @Override
    public void onCollision(Collisionable other) {
        Logger.trace(this + " had a collision");
        other.hp -= 10;
        deleteSelf();
    }
}
