package com.superkiment.client.graphics.ui;

import org.joml.Vector2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class UIElementTest {

    private UIElement element;

    @BeforeEach
    void setUp() {
        element = new UIElement(new Vector2d(10, 20), 5);
    }

    // ───────────────────── Construction ──────────────────────────────────

    @Test
    void constructor_setsPosition() {
        assertEquals(10, element.pos.x, 1e-9);
        assertEquals(20, element.pos.y, 1e-9);
    }

    @Test
    void constructor_setsZIndex() {
        assertEquals(5, element.getZIndex());
    }

    @Test
    void constructor_defaultDimIs100x100() {
        assertEquals(100, element.dim.x, 1e-9, "dim.x par défaut = 100");
        assertEquals(100, element.dim.y, 1e-9, "dim.y par défaut = 100");
    }

    @Test
    void constructor_isClickableIsFalseByDefault() {
        assertFalse(element.isClickable, "Un UIElement de base ne doit pas être cliquable");
    }

    @Test
    void constructor_shapeModelIsInitialized() {
        assertNotNull(element.shapeModel);
    }

    @Test
    void constructor_childrenListIsEmpty() {
        assertTrue(element.children.isEmpty());
    }

    // ───────────────────── Hiérarchie parent/enfant ───────────────────────

    @Test
    void addChild_setsParentReference() {
        UIElement child = new UIElement(new Vector2d(0, 0), 0);
        element.addChild(child);

        assertSame(element, child.parent, "L'enfant doit référencer son parent");
    }

    @Test
    void addChild_childAppearsInList() {
        UIElement child = new UIElement(new Vector2d(0, 0), 0);
        element.addChild(child);

        assertTrue(element.children.contains(child));
        assertEquals(1, element.children.size());
    }

    @Test
    void addChild_multipleChildren_allPresent() {
        UIElement c1 = new UIElement(new Vector2d(0, 0), 0);
        UIElement c2 = new UIElement(new Vector2d(0, 0), 0);
        UIElement c3 = new UIElement(new Vector2d(0, 0), 0);

        element.addChild(c1);
        element.addChild(c2);
        element.addChild(c3);

        assertEquals(3, element.children.size());
    }

    @Test
    void addYourselfAndChildrenToList_collectsAll() {
        UIElement child1 = new UIElement(new Vector2d(0, 0), 0);
        UIElement child2 = new UIElement(new Vector2d(0, 0), 0);
        element.addChild(child1);
        element.addChild(child2);

        java.util.List<UIElement> list = new java.util.ArrayList<>();
        element.addYourselfAndChildrenToThisList(list);

        assertEquals(3, list.size(), "Parent + 2 enfants = 3 éléments");
        assertTrue(list.contains(element));
        assertTrue(list.contains(child1));
        assertTrue(list.contains(child2));
    }

    // ───────────────────── isClicked ─────────────────────────────────────

    @Test
    void isClicked_whenNotClickable_alwaysFalse() {
        element.isClickable = false;
        // Ajouter une shape pour que isPointInShape puisse répondre
        element.shapeModel.addShape(new com.superkiment.common.shapes.Shape(
                new Vector2d(0, 0), new Vector2d(100, 100),
                com.superkiment.common.shapes.Shape.ShapeType.RECT));

        // Même un clic dans les bounds doit retourner false si !isClickable
        assertFalse(element.isClicked((float) element.pos.x, (float) element.pos.y));
    }

    // ───────────────────── onClick (smoke test) ───────────────────────────

    @Test
    void onClick_printsToStdout() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        try {
            element.onClick();
        } finally {
            System.setOut(original);
        }
        assertFalse(buf.toString().isEmpty(), "onClick doit imprimer quelque chose");
    }
}
