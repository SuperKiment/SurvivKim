package com.superkiment.client.graphics.ui.shapemodels;

import com.superkiment.common.collisions.Collisionable;
import com.superkiment.common.shapes.Shape;
import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;
import org.joml.Vector3d;

public class SMHealthBar extends ShapeModel {
    private final Shape bar;
    private final int baseWidth = 45;
    private float invMaxHP = 0;

    public SMHealthBar() {
        super();

        this.addShape(new Shape(new Vector2d(0, 50), new Vector2d(50, 10), Shape.ShapeType.RECT, new Vector3d(.1, .1, .1)));
        this.bar = new Shape(new Vector2d(0, 50), new Vector2d(baseWidth, 6), Shape.ShapeType.RECT, new Vector3d(1, 0, 0));
        this.addShape(bar);
    }

    @Override
    public void update(Collisionable collisionable) {
        if (invMaxHP == 0) invMaxHP = 1f / collisionable.maxHP;
        bar.dimensions.x = baseWidth * collisionable.hp * invMaxHP;
    }
}
