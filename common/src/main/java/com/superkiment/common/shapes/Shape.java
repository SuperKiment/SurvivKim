package com.superkiment.common.shapes;

import org.joml.Vector2d;
import org.joml.Vector3d;

public class Shape {
    public enum ShapeType {
        RECT, CIRCLE, TRIANGLE,
        RECT_OUTLINE, CIRCLE_OUTLINE, TRIANGLE_OUTLINE
    }

    public Vector3d color = new Vector3d(1f, 0f, 0f);
    public Vector2d position;
    public ShapeType shapeType = ShapeType.CIRCLE;

    public Vector2d dimensions = null;
    public final int segments = 10;
    public final float lineWidth = 3f;

    // Attributs pour le texte
    public String text = "";
    public float fontSize = 32f;
    public Vector3d textColor = new Vector3d(0f, 0f, 0f);
    public String fontName = "minecraft"; // Nom de la police à utiliser

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

    public void setText(String text) {
        this.text = text;
    }

    public void setText(String text, float fontSize) {
        this.text = text;
        this.fontSize = fontSize;
    }

    public void setText(String text, float fontSize, Vector3d textColor) {
        this.text = text;
        this.fontSize = fontSize;
        this.textColor = textColor;
    }

    public void setText(String text, float fontSize, Vector3d textColor, String fontName) {
        this.text = text;
        this.fontSize = fontSize;
        this.textColor = textColor;
        this.fontName = fontName;
    }

    public void setFont(String fontName) {
        this.fontName = fontName;
    }

    public boolean hasText() {
        return text != null && !text.isEmpty();
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