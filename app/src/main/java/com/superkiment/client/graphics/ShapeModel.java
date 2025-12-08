package com.superkiment.client.graphics;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;

public class ShapeModel {
    public ArrayList<Shape> shapes;

    public ShapeModel() {
        shapes = new ArrayList<Shape>();
    }

    public void renderModel() {
        glPushMatrix();
        for (Shape shape : shapes) {
            //Render all shapes
            shape.draw();
        }
        glPopMatrix();
    }

    public void addShape(Shape s) {
        shapes.add(s);
    }
}
