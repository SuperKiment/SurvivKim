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
        RECT, CIRCLE, TRIANGLE,
        RECT_OUTLINE, CIRCLE_OUTLINE, TRIANGLE_OUTLINE
    }

    public static Vector3d color = new Vector3d(1f, 0f, 0f);

    public Vector2d position;
    protected final ShapeType shapeType;

    // Utilisé pour Cercle et Rectangle
    public final Vector2d dimensions;
    public final int segments = 10;
    public final float lineWidth = 3f;

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
        glTranslated(position.x, position.y, 0);
        glColor3d(color.x, color.y, color.z);

        switch (shapeType) {
            case RECT -> {
                glBegin(GL_QUADS);
                glVertex2f(-(float) dimensions.x / 2, -(float) dimensions.y / 2);
                glVertex2f((float) dimensions.x / 2, -(float) dimensions.y / 2);
                glVertex2f((float) dimensions.x / 2, (float) dimensions.y / 2);
                glVertex2f(-(float) dimensions.x / 2, (float) dimensions.y / 2);
                glEnd();
            }
            case CIRCLE -> {
                glBegin(GL_TRIANGLE_FAN);
                glVertex2f(0, 0); // Centre

                for (int i = 0; i <= segments; i++) {
                    float angle = (float) (2.0 * Math.PI * i / segments);
                    double x = Math.cos(angle) * dimensions.x;
                    double y = Math.sin(angle) * dimensions.x;
                    glVertex2d(x, y);
                }
                glEnd();
            }
            case TRIANGLE -> {
            }
            case RECT_OUTLINE -> {
                glLineWidth(lineWidth);
                glColor3d(0, 0, 0);

                glBegin(GL_LINE_LOOP);
                glVertex2d(-dimensions.x / 2, -dimensions.y / 2);
                glVertex2d(dimensions.x / 2, -dimensions.y / 2);
                glVertex2d(dimensions.x / 2, dimensions.y / 2);
                glVertex2d(-dimensions.x / 2, dimensions.y / 2);
                glEnd();

                glPopMatrix();
            }
            case CIRCLE_OUTLINE -> {
                glLineWidth(lineWidth);
                glColor3d(0, 0, 0);

                glBegin(GL_LINE_LOOP);
                for (int i = 0; i < segments; i++) {
                    double angle = 2.0 * Math.PI * i / segments;
                    double x = Math.cos(angle) * dimensions.x;
                    double y = Math.sin(angle) * dimensions.x;
                    glVertex2d(x, y);
                }
                glEnd();
            }
            default -> throw new IllegalStateException("Unexpected value: " + shapeType);
        }
        glPopMatrix();
    }

    public void setColor(float r, float g, float b) {
        color = new Vector3d(r, g, b);
    }
}
