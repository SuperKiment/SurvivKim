package com.superkiment.common.entities;

import com.superkiment.client.Time;
import org.joml.Vector2d;

public class Entity {
    public Vector2d pos, dir;
    public float speed = 200f;
    public String id;
    public String name = "NoName";

    public Entity() {
        this(new Vector2d(200f, 200f));
    }

    public Entity(Vector2d pos) {
        this.pos = new Vector2d(pos.x, pos.y);
        this.dir = new Vector2d(1, 0);
    }

    public void turnToDirection(Vector2d dir) {
        this.dir.set(dir);
    }

    public void moveInDirection() {
        Vector2d mvt = new Vector2d(dir.x, dir.y);
        mvt
                .normalize()
                .mul(speed)
                .mul(Time.getDeltaTime());
        pos.add(mvt);
    }

    public void moveToPosition(Vector2d pos) {
        this.pos.x = pos.x;
        this.pos.y = pos.y;
    }
}