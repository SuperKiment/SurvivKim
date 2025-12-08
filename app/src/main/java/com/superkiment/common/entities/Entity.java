package com.superkiment.common.entities;

import com.superkiment.client.Time;
import com.superkiment.client.graphics.Shape;
import com.superkiment.client.graphics.ShapeModel;
import org.joml.Vector2d;

import static com.superkiment.common.utils.Vector.Lerp;
import static com.superkiment.common.utils.Vector.LerpRotation;

public class Entity {
    public Vector2d pos, posLerp,
            dirLookTarget, dirLookLerp, dirDepl;
    public float speed = 200f;
    public String id;
    public String name = "NoName";
    public ShapeModel shapeModel;

    public boolean moveFromInput = false;

    public Entity() {
        this(new Vector2d(200f, 200f));
    }

    public Entity(Vector2d pos) {
        this.pos = new Vector2d(pos.x, pos.y);
        this.posLerp = new Vector2d(pos.x, pos.y);
        this.dirLookTarget = new Vector2d(0, 1);
        this.dirLookLerp = new Vector2d(0, 1);
        this.dirDepl = new Vector2d(0, 0);

        shapeModel = new ShapeModel();
        shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(100, 100), Shape.ShapeType.RECT));
        shapeModel.addShape(new Shape(new Vector2d(50, 0), new Vector2d(50, 50), Shape.ShapeType.RECT));
    }

    public void update() {
        updateMovement();
        updateLerp();
    }

    public void turnToDirection(Vector2d dir) {
        this.dirLookTarget.set(dir);
    }

    private void updateMovement() {
        if (!moveFromInput) return;

        dirLookTarget.set(dirDepl.x, dirDepl.y);

        Vector2d mvt = new Vector2d(dirDepl.x, dirDepl.y);
        mvt
                .normalize()
                .mul(speed)
                .mul(Time.getDeltaTime());
        pos.add(mvt);

        moveFromInput = false;
    }

    private void updateLerp() {
        dirLookLerp = LerpRotation(dirLookLerp, dirLookTarget, 0.2);
        posLerp = Lerp(posLerp, pos, 0.3);
    }

    public void moveToPosition(Vector2d pos) {
        this.pos.x = pos.x;
        this.pos.y = pos.y;
    }
}