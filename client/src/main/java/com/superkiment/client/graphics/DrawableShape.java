package com.superkiment.client.graphics;

import com.superkiment.common.shapes.Shape;

import static org.lwjgl.opengl.GL11.*;

public class DrawableShape extends Shape {

    public DrawableShape(Shape shape) {
        super(shape.position, shape.dimensions, shape.shapeType);
    }


}
