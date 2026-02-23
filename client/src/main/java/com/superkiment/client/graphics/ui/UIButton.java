package com.superkiment.client.graphics.ui;

import com.superkiment.common.shapes.Shape;
import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;

public class UIButton extends UIElement {

    public UIButton(Vector2d pos, Vector2d dim, int z) {
        super(pos, z);
        this.isClickable = true;
        this.dim = dim;
        this.shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(50, 50), Shape.ShapeType.RECT));
    }

    public UIButton(Vector2d pos, Vector2d dim, int z, ShapeModel shapeModel) {
        this(pos, dim, z);
        this.shapeModel = shapeModel;
    }
}
