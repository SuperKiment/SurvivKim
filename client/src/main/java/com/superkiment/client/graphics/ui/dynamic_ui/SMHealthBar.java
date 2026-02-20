package com.superkiment.client.graphics.ui.dynamic_ui;

import com.superkiment.common.collisions.Collisionable;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Projectile;
import com.superkiment.common.shapes.Shape;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class SMHealthBar extends ShapeModel_UI {
    public static List<Class<?>> applyToClass = new ArrayList<>();
    public static List<Class<?>> doNotApplyToClass = new ArrayList<>();

    private final Shape bar;
    private final int baseWidth = 45;
    private float invMaxHP = 0;

    static {
        applyToClass.add(Entity.class);
        doNotApplyToClass.add(Projectile.class);
    }

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
        bar.position.x = -(baseWidth - bar.dimensions.x) * 0.5;

        if (bar.dimensions.x <= 0) {
            bar.dimensions.x = 0;
            bar.position.x = 0;
        }
    }
}
