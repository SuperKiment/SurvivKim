package com.superkiment.client.graphics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;

public class ShapeModel {
    public List<Shape> shapes;

    public ShapeModel() {
        shapes = new CopyOnWriteArrayList<>();
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
