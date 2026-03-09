package com.superkiment.common.shapes;

import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShapeTest {

    // ─────────────────────────── RECT ─────────────────────────────────────

    @Test
    void rect_centerPoint_isInside() {
        // Rectangle centré en (0,0), 100×50
        Shape rect = new Shape(new Vector2d(0, 0), new Vector2d(100, 50), Shape.ShapeType.RECT);
        assertTrue(rect.isPointInShape(new Vector2d(0, 0)), "Le centre doit être à l'intérieur");
    }

    @Test
    void rect_cornerPoint_isInside() {
        Shape rect = new Shape(new Vector2d(0, 0), new Vector2d(100, 50), Shape.ShapeType.RECT);
        // coin exactement sur la bordure
        assertTrue(rect.isPointInShape(new Vector2d(50, 25)));
    }

    @Test
    void rect_outsidePoint_isNotInside() {
        Shape rect = new Shape(new Vector2d(0, 0), new Vector2d(100, 50), Shape.ShapeType.RECT);
        assertFalse(rect.isPointInShape(new Vector2d(60, 0)), "Un point trop à droite ne doit pas être dedans");
    }

    @Test
    void rect_outline_behavesLikeRect_forPointTest() {
        Shape outline = new Shape(new Vector2d(0, 0), new Vector2d(80, 40), Shape.ShapeType.RECT_OUTLINE);
        assertTrue(outline.isPointInShape(new Vector2d(0, 0)), "RECT_OUTLINE se comporte comme RECT pour les tests de point");
    }

    // ─────────────────────────── CIRCLE ───────────────────────────────────

    @Test
    void circle_centerPoint_isInside() {
        // dimensions.x = rayon = 30
        Shape circle = new Shape(new Vector2d(0, 0), new Vector2d(30, 30), Shape.ShapeType.CIRCLE);
        assertTrue(circle.isPointInShape(new Vector2d(0, 0)));
    }

    @Test
    void circle_pointOnRadius_isInside() {
        Shape circle = new Shape(new Vector2d(0, 0), new Vector2d(30, 30), Shape.ShapeType.CIRCLE);
        // Point à distance 29 du centre → dedans
        assertTrue(circle.isPointInShape(new Vector2d(29, 0)));
    }

    @Test
    void circle_pointOutsideRadius_isNotInside() {
        Shape circle = new Shape(new Vector2d(0, 0), new Vector2d(30, 30), Shape.ShapeType.CIRCLE);
        assertFalse(circle.isPointInShape(new Vector2d(31, 0)));
    }

    // ─────────────────────────── Texte ────────────────────────────────────

    @Test
    void setText_hasText_returnsTrue() {
        Shape s = new Shape(new Vector2d(0, 0), new Vector2d(10, 10), Shape.ShapeType.RECT);
        assertFalse(s.hasText(), "Pas de texte par défaut");
        s.setText("Hello");
        assertTrue(s.hasText(), "hasText() doit être true après setText()");
    }

    @Test
    void setName_name_isSetCorrectly() {
        Shape s = new Shape(new Vector2d(0, 0), new Vector2d(10, 10), Shape.ShapeType.RECT);
        s.setName("ma_shape");
        assertEquals("ma_shape", s.name);
    }

    @Test
    void setColor_updatesColorValues() {
        Shape s = new Shape(new Vector2d(0, 0), new Vector2d(10, 10), Shape.ShapeType.RECT);
        s.setColor(0.5f, 0.25f, 0.1f);
        assertEquals(0.5f, (float) s.color.x, 1e-6f);
        assertEquals(0.25f, (float) s.color.y, 1e-6f);
        assertEquals(0.1f, (float) s.color.z, 1e-6f);
    }
}
