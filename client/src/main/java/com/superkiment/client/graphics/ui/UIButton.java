package com.superkiment.client.graphics.ui;

import com.superkiment.common.shapes.Shape;
import org.joml.Vector2d;
import org.joml.Vector3d;

public class UIButton extends UIElement {

    public UIButton(Vector2d pos, Vector2d dim, int z) {
        super(pos, z);
        this.isClickable = true;
        this.dim = dim;

        this.shapeModel.addShape(
                new Shape(new Vector2d(0, 0), dim, Shape.ShapeType.RECT, new Vector3d(0, 1, 0))
        );
    }
}
