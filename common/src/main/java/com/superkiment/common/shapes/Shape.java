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
    public Vector2d textOffset = new Vector2d(0, 0);
    public String name = "";

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

    public void setName(String name) {
        this.name = name;
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

    public void setTextOffset(double x, double y) {
        this.textOffset = new Vector2d(x, y);
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

    public boolean isPointInShape(Vector2d point) {
        ShapeType base = getBaseType(this.shapeType);
        return switch (base) {
            case RECT -> {
                double halfW = dimensions.x / 2.0;
                double halfH = dimensions.y / 2.0;
                yield point.x >= position.x - halfW && point.x <= position.x + halfW
                        && point.y >= position.y - halfH && point.y <= position.y + halfH;
            }
            case CIRCLE -> point.distanceSquared(position) <= dimensions.x * dimensions.x;

            case TRIANGLE -> {
                // Triangle isocèle centré sur position, base = dimensions.x, hauteur = dimensions.y
                double halfW = dimensions.x / 2.0;
                double halfH = dimensions.y / 2.0;

                // Sommets : haut-centre, bas-gauche, bas-droite
                double x1 = position.x, y1 = position.y - halfH;
                double x2 = position.x - halfW, y2 = position.y + halfH;
                double x3 = position.x + halfW, y3 = position.y + halfH;

                // Calcul via les signes des produits vectoriels
                double d1 = sign(point.x, point.y, x1, y1, x2, y2);
                double d2 = sign(point.x, point.y, x2, y2, x3, y3);
                double d3 = sign(point.x, point.y, x3, y3, x1, y1);

                boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
                boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
                yield !(hasNeg && hasPos);
            }
            default -> false;
        };
    }

    private double sign(double px, double py, double ax, double ay, double bx, double by) {
        return (px - bx) * (ay - by) - (ax - bx) * (py - by);
    }
}