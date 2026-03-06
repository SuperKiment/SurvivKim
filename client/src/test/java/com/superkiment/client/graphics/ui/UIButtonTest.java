package com.superkiment.client.graphics.ui;

import com.superkiment.common.shapes.Shape;
import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UIButtonTest {

    private UIButton button;

    @BeforeEach
    void setUp() {
        // Bouton positionné à (100, 100), dimension 50×50, z=1
        button = new UIButton(new Vector2d(100, 100), new Vector2d(50, 50), 1);
    }

    // ───────────────────── Construction ──────────────────────────────────

    @Test
    void constructor_isClickableTrue() {
        assertTrue(button.isClickable, "Un UIButton doit être cliquable par défaut");
    }

    @Test
    void constructor_dimensionSetCorrectly() {
        assertEquals(50, button.dim.x, 1e-9);
        assertEquals(50, button.dim.y, 1e-9);
    }

    @Test
    void constructor_positionSetCorrectly() {
        assertEquals(100, button.pos.x, 1e-9);
        assertEquals(100, button.pos.y, 1e-9);
    }

    @Test
    void constructor_hasAtLeastOneShape() {
        assertFalse(button.shapeModel.shapes.isEmpty(),
                "Le UIButton doit avoir au moins une shape pour le hit-test");
    }

    @Test
    void constructor_withCustomShapeModel_usesCustomModel() {
        ShapeModel custom = new ShapeModel();
        custom.addShape(new Shape(new Vector2d(0, 0), new Vector2d(80, 80), Shape.ShapeType.CIRCLE));

        UIButton customBtn = new UIButton(new Vector2d(0, 0), new Vector2d(80, 80), 0, custom);
        assertSame(custom, customBtn.shapeModel, "Le ShapeModel custom doit être utilisé");
        assertEquals(Shape.ShapeType.CIRCLE,
                customBtn.shapeModel.shapes.get(customBtn.shapeModel.shapes.size() - 1).shapeType,
                "La dernière shape doit être le cercle du custom model");
    }

    // ───────────────────── isClicked ─────────────────────────────────────

    /**
     * UIButton utilise la shape RECT 50×50 centrée en (0,0) relative à pos.
     * isPointInShape teste si le point (x - pos.x, y - pos.y) est dans la shape.
     * Shape RECT(0,0) 50×50 : demi-largeur=25, demi-hauteur=25
     * → points valides : pos.x-25 ≤ x ≤ pos.x+25, pos.y-25 ≤ y ≤ pos.y+25
     */
    @Test
    void isClicked_insideBounds_returnsTrue() {
        // Clic exactement sur le centre du bouton
        assertTrue(button.isClicked(100f, 100f), "Un clic au centre doit être détecté");
    }

    @Test
    void isClicked_nearCenter_returnsTrue() {
        // Clic légèrement décalé du centre, toujours dans la shape
        assertTrue(button.isClicked(110f, 110f));
    }

    @Test
    void isClicked_outsideBounds_returnsFalse() {
        // Très loin du bouton
        assertFalse(button.isClicked(500f, 500f), "Un clic loin du bouton doit être ignoré");
    }

    @Test
    void isClicked_justOutsideEdge_returnsFalse() {
        // pos.x + 26 > demi-largeur 25 → hors de la shape
        assertFalse(button.isClicked(127f, 100f),
                "Un clic juste en dehors du bord droit doit être ignoré");
    }

    @Test
    void isClicked_zeroZeroButton_centerClick_returnsTrue() {
        UIButton btn = new UIButton(new Vector2d(0, 0), new Vector2d(50, 50), 0);
        assertTrue(btn.isClicked(0f, 0f), "Un clic au centre (0,0) doit être détecté");
    }

    // ───────────────────── Héritage UIElement ────────────────────────────

    @Test
    void button_inheritsFromUIElement() {
        assertInstanceOf(UIElement.class, button, "UIButton doit hériter de UIElement");
    }

    @Test
    void addChild_worksOnButton() {
        UIElement child = new UIElement(new Vector2d(0, 0), 0);
        button.addChild(child);

        assertEquals(1, button.children.size());
        assertSame(button, child.parent);
    }
}
