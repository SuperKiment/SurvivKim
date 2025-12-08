package com.superkiment.client.graphics;

import org.joml.Vector2d;
import org.joml.Vector3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glVertex2f;

public class Shape {
    public enum ShapeType {
        RECT, CIRCLE, TRIANGLE
    }

    public static Vector3d color = new Vector3d(1f, 0f, 0f);

    public Vector2d position;
    private ShapeType shapeType = ShapeType.RECT;

    // Utilisé pour Cercle et Rectangle
    private final Vector2d dimensions;

    public Shape(Vector2d pos, Vector2d dim, ShapeType st) {
        this.position = pos;
        this.dimensions = dim;
        this.shapeType = st;
    }

    public Shape() {
        this(new Vector2d(100d, 100d), new Vector2d(100d, 200d), ShapeType.RECT);
    }

    public void draw() {
        glPushMatrix();
        glTranslatef((float)position.x, (float)position.y, 0);
        glColor3f((float)color.x, (float)color.y, (float)color.z);

        switch (shapeType) {
            case RECT -> {
                glBegin(GL_QUADS);
                glVertex2f(-(float)dimensions.x / 2, -(float)dimensions.y / 2);
                glVertex2f((float)dimensions.x / 2, -(float)dimensions.y / 2);
                glVertex2f((float)dimensions.x / 2, (float)dimensions.y/ 2);
                glVertex2f(-(float)dimensions.x / 2, (float)dimensions.y/ 2);
                glEnd();
            }
            case CIRCLE, TRIANGLE -> {
            }
            default -> throw new IllegalStateException("Unexpected value: " + shapeType);
        }
        glPopMatrix();
    }

    public void setColor(float r, float g, float b) {
        color = new Vector3d(r, g, b);
    }
}
