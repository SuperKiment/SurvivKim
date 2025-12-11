package com.superkiment.common.entities;

import com.superkiment.client.Time;
import com.superkiment.client.graphics.Renderable;
import com.superkiment.client.graphics.Shape;
import com.superkiment.client.graphics.ShapeModel;
import com.superkiment.common.collisions.CollisionsManager;
import com.superkiment.common.collisions.CollisionShape;
import org.joml.Vector2d;

import java.util.UUID;

import static com.superkiment.common.utils.Vector.Lerp;
import static com.superkiment.common.utils.Vector.LerpRotation;

public class Entity extends Renderable {
    public Vector2d pos, posLerp,
            dirLookTarget, dirLookLerp, dirDepl;
    public float speed = 200f;
    public String id;
    public String name = "NoName";

    public boolean moveFromInput = false;
    public boolean dirtyPosition = false;

    private CollisionsManager collisionManager;

    public Entity() {
        this(new Vector2d(200f, 200f));
    }

    public Entity(Vector2d pos) {
        this.id = UUID.randomUUID().toString();
        this.pos = new Vector2d(pos.x, pos.y);
        this.posLerp = new Vector2d(pos.x, pos.y);
        this.dirLookTarget = new Vector2d(0, 1);
        this.dirLookLerp = new Vector2d(0, 1);
        this.dirDepl = new Vector2d(0, 0);

        shapeModel = new ShapeModel();

        collisionManager = new CollisionsManager(this);
        collisionManager.addCollisionShape(
                new CollisionShape(
                        new Vector2d(0, 0),
                        new Vector2d(25, 25),
                        Shape.ShapeType.CIRCLE_OUTLINE
                )
        );
    }

    public void update() {
        dirtyPosition = false;
        updateMovement();
        updateCollisions();
    }

    public void turnToDirection(Vector2d dir) {
        this.dirLookTarget.set(dir);
    }

    protected void updateMovement() {
        if (!moveFromInput) return;

        dirLookTarget.set(dirDepl.x, dirDepl.y);

        Vector2d mvt = new Vector2d(dirDepl.x, dirDepl.y);
        mvt
                .normalize()
                .mul(speed)
                .mul(Time.getDeltaTime());
        pos.add(mvt);

        dirtyPosition = true;

        moveFromInput = false;
    }

    protected void updateCollisions() {

    }

    public void updateLerp() {
        dirLookLerp = LerpRotation(dirLookLerp, dirLookTarget, 0.2);
        posLerp = Lerp(posLerp, pos, 0.3);
    }
}