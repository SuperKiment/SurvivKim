package com.superkiment.client.graphics.ui;

import com.superkiment.common.shapes.Shape;
import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;
import org.joml.Vector3d;

public class UIElement {
    public Vector2d pos, dim;
    public ShapeModel shapeModel;

    protected UIElement() {
    }

    public UIElement(Vector2d pos, Vector2d dim) {
        this.pos = pos;
        this.dim = dim;
        this.shapeModel = new ShapeModel();

        shapeModel.addShape(new Shape(pos, dim, Shape.ShapeType.RECT, new Vector3d(.5, .5, .5)));
    }

    public void Render() {

    }

    public boolean isClicked(float x, float y) {
        Vector2d topLeft = new Vector2d(pos.x - dim.x / 2, pos.y - dim.y / 2);
        Vector2d bottomRight = new Vector2d(pos.x + dim.x / 2, pos.y + dim.y / 2);

        return (x > topLeft.x && x < bottomRight.x && y > topLeft.y && y < bottomRight.y);
    }
}
