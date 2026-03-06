package com.superkiment.common.shapes;

import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShapeModelTest {

    @Test
    void addShape_increasesShapeCount() {
        ShapeModel model = new ShapeModel();
        assertEquals(0, model.shapes.size(), "Doit être vide au départ");

        model.addShape(new Shape(new Vector2d(0, 0), new Vector2d(20, 20), Shape.ShapeType.RECT));
        assertEquals(1, model.shapes.size());

        model.addShape(new Shape(new Vector2d(50, 50), new Vector2d(10, 10), Shape.ShapeType.CIRCLE));
        assertEquals(2, model.shapes.size());
    }

    @Test
    void isPointInShape_delegatesToChildren() {
        ShapeModel model = new ShapeModel();
        model.addShape(new Shape(new Vector2d(0, 0), new Vector2d(50, 50), Shape.ShapeType.RECT));

        assertTrue(model.isPointInShape(new Vector2d(0, 0)),   "Centre → dedans");
        assertFalse(model.isPointInShape(new Vector2d(100, 0)), "Trop loin → dehors");
    }

    @Test
    void isPointInShape_anyShapeMatches_returnsTrue() {
        ShapeModel model = new ShapeModel();
        // Shape 1 : rect en haut à gauche
        model.addShape(new Shape(new Vector2d(-50, 0), new Vector2d(20, 20), Shape.ShapeType.RECT));
        // Shape 2 : rect en bas à droite
        model.addShape(new Shape(new Vector2d(50, 0), new Vector2d(20, 20), Shape.ShapeType.RECT));

        assertTrue(model.isPointInShape(new Vector2d(-50, 0)), "Shape 1 doit matcher");
        assertTrue(model.isPointInShape(new Vector2d(50, 0)),  "Shape 2 doit matcher");
        assertFalse(model.isPointInShape(new Vector2d(0, 0)),   "Ni l'un ni l'autre");
    }

    @Test
    void getShapeByName_existingName_returnsShape() {
        ShapeModel model = new ShapeModel();
        Shape s = new Shape(new Vector2d(0, 0), new Vector2d(10, 10), Shape.ShapeType.RECT);
        s.setName("healthbar");
        model.addShape(s);

        assertSame(s, model.getShapeByName("healthbar"), "Doit retourner exactement la shape nommée");
    }

    @Test
    void getShapeByName_missingName_returnsNull() {
        ShapeModel model = new ShapeModel();
        assertNull(model.getShapeByName("inexistant"));
    }

    @Test
    void fromJson_parsesSimpleRect() {
        String json = """
            {
              "shapes": [
                {
                  "type": "RECT",
                  "position":   { "x": 0, "y": 0 },
                  "dimensions": { "x": 80, "y": 50 },
                  "color":      { "r": 1.0, "g": 0.0, "b": 0.0 }
                }
              ]
            }
            """;

        ShapeModel model = ShapeModel.fromJson(json);
        assertEquals(1, model.shapes.size(), "Doit contenir une shape");
        Shape s = model.shapes.get(0);
        assertEquals(Shape.ShapeType.RECT, s.shapeType);
        assertEquals(80, s.dimensions.x, 1e-9);
        assertEquals(50, s.dimensions.y, 1e-9);
    }
}
