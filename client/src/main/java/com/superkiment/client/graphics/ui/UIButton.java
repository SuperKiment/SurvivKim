package com.superkiment.client.graphics.ui;

import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;

public class UIButton extends UIElement {

    public UIButton(Vector2d pos, Vector2d dim, int z) {
        super(pos, z);
        this.isClickable = true;
        this.dim = dim;

        try {
            this.shapeModel = ShapeModel.fromJsonFile("/assets/shapemodels/random.json");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
