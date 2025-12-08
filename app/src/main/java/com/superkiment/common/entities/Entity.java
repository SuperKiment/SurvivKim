package com.superkiment.common.entities;

import com.superkiment.client.Time;
import com.superkiment.client.graphics.Shape;
import com.superkiment.client.graphics.ShapeModel;
import org.joml.Vector2d;

public class Entity {
    public Vector2d pos, dirLook, dirDepl;
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
        this.dirLook = new Vector2d(0, 1);
        this.dirDepl = new Vector2d(0, 0);

        shapeModel = new ShapeModel();
        shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(100, 100), Shape.ShapeType.RECT));
        shapeModel.addShape(new Shape(new Vector2d(50, 0), new Vector2d(50, 50), Shape.ShapeType.RECT));
    }

    public void update() {
        updateMovement();
    }

    public void turnToDirection(Vector2d dir) {
        this.dirLook.set(dir);
    }

    private void updateMovement() {
        if (!moveFromInput) return;

        dirLook.set(dirDepl.x, dirDepl.y);

        Vector2d mvt = new Vector2d(dirDepl.x, dirDepl.y);
        mvt
                .normalize()
                .mul(speed)
                .mul(Time.getDeltaTime());
        pos.add(mvt);

        moveFromInput = false;
    }

    public void moveToPosition(Vector2d pos) {
        this.pos.x = pos.x;
        this.pos.y = pos.y;
    }
}