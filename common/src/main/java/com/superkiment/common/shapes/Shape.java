package com.superkiment.common.shapes;

import org.joml.Vector2d;
import org.joml.Vector3d;

/**
 * Une classe qui permet de stocker et afficher des données sur des formes, permet également de les afficher.
 */
public class Shape {
    public enum ShapeType {
        RECT, CIRCLE, TRIANGLE,
        RECT_OUTLINE, CIRCLE_OUTLINE, TRIANGLE_OUTLINE
    }

    public Vector3d color = new Vector3d(1f, 0f, 0f);
    public Vector2d position;
    public ShapeType shapeType = ShapeType.CIRCLE;

    // Utilisé pour Cercle et Rectangle
    public Vector2d dimensions = null;
    public final int segments = 10;
    public final float lineWidth = 3f;

    public Shape(Vector2d pos, Vector2d dim, ShapeType st) {
        this.position = pos;
        this.dimensions = dim;
        this.shapeType = st;
    }

    public Shape(Vector2d pos, Vector2d dim, ShapeType st, Vector3d col) {
        this.position = pos;
        this.dimensions = dim;
        this.shapeType = st;
        this.color = col;
    }

    private Shape() {
    }


    public void setColor(float r, float g, float b) {
        color = new Vector3d(r, g, b);
    }

    protected ShapeType getBaseType(ShapeType type) {
        return switch (type) {
            case RECT_OUTLINE -> ShapeType.RECT;
            case CIRCLE_OUTLINE -> ShapeType.CIRCLE;
            case TRIANGLE_OUTLINE -> ShapeType.TRIANGLE;
            default -> type;
        };
    }
}
