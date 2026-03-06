package com.superkiment.common.collisions;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Player;
import com.superkiment.common.shapes.Shape;
import org.joml.Vector2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour la détection de collision.
 * Vérifie que CollisionShape.isInCollisionWith() retourne les bons résultats.
 */
public class CollisionShapeTest {

    // ─────────────────── Helpers ─────────────────────────────────────────

    /** Crée une entité avec UNE CollisionShape rectangulaire. */
    private Entity entityWithRect(double cx, double cy, double w, double h) {
        Entity e = new Entity(new Vector2d(cx, cy));
        e.collisionsManager.addCollisionShape(
                new CollisionShape(
                        new Vector2d(0, 0),
                        new Vector2d(w, h),
                        Shape.ShapeType.RECT_OUTLINE,
                        e
                )
        );
        return e;
    }

    /** Crée une entité avec UNE CollisionShape circulaire. */
    private Entity entityWithCircle(double cx, double cy, double radius) {
        Entity e = new Entity(new Vector2d(cx, cy));
        e.collisionsManager.addCollisionShape(
                new CollisionShape(
                        new Vector2d(0, 0),
                        new Vector2d(radius, radius),
                        Shape.ShapeType.CIRCLE_OUTLINE,
                        e
                )
        );
        return e;
    }

    // ─────────────────── Rect ↔ Rect ─────────────────────────────────────

    @Test
    void rectRect_overlapping_detectsCollision() {
        Entity a = entityWithRect(0, 0, 50, 50);
        Entity b = entityWithRect(25, 25, 50, 50); // se chevauchent

        CollisionShape shapeA = a.collisionsManager.collisionShapes.get(0);
        CollisionShape shapeB = b.collisionsManager.collisionShapes.get(0);

        assertTrue(shapeA.isInCollisionWith(shapeB), "Deux rects qui se chevauchent doivent être en collision");
    }

    @Test
    void rectRect_adjacent_noCollision() {
        // B commence exactement là où A finit → pas de chevauchement
        Entity a = entityWithRect(0,  0,   50, 50);
        Entity b = entityWithRect(51, 0,   50, 50);

        CollisionShape shapeA = a.collisionsManager.collisionShapes.get(0);
        CollisionShape shapeB = b.collisionsManager.collisionShapes.get(0);

        assertFalse(shapeA.isInCollisionWith(shapeB), "Deux rects côte à côte ne doivent pas être en collision");
    }

    @Test
    void rectRect_farApart_noCollision() {
        Entity a = entityWithRect(0,   0,   50, 50);
        Entity b = entityWithRect(500, 500, 50, 50);

        CollisionShape shapeA = a.collisionsManager.collisionShapes.get(0);
        CollisionShape shapeB = b.collisionsManager.collisionShapes.get(0);

        assertFalse(shapeA.isInCollisionWith(shapeB));
    }

    @Test
    void rectRect_samePosition_detectsCollision() {
        Entity a = entityWithRect(100, 100, 40, 40);
        Entity b = entityWithRect(100, 100, 40, 40); // même position

        CollisionShape shapeA = a.collisionsManager.collisionShapes.get(0);
        CollisionShape shapeB = b.collisionsManager.collisionShapes.get(0);

        assertTrue(shapeA.isInCollisionWith(shapeB), "Même position → collision certaine");
    }

    // ─────────────────── Circle ↔ Circle ─────────────────────────────────

    @Test
    void circleCircle_overlapping_detectsCollision() {
        Entity a = entityWithCircle(0,  0, 30);
        Entity b = entityWithCircle(40, 0, 30); // distance = 40, somme des rayons = 60

        CollisionShape shapeA = a.collisionsManager.collisionShapes.get(0);
        CollisionShape shapeB = b.collisionsManager.collisionShapes.get(0);

        assertTrue(shapeA.isInCollisionWith(shapeB));
    }

    @Test
    void circleCircle_tooFar_noCollision() {
        Entity a = entityWithCircle(0,   0, 30);
        Entity b = entityWithCircle(100, 0, 30); // distance = 100, somme = 60

        CollisionShape shapeA = a.collisionsManager.collisionShapes.get(0);
        CollisionShape shapeB = b.collisionsManager.collisionShapes.get(0);

        assertFalse(shapeA.isInCollisionWith(shapeB));
    }

    // ─────────────────── findCollisionsWithData ───────────────────────────

    @Test
    void findCollisions_twoOverlappingPlayers_returnsCollision() {
        EntitiesManager em = new EntitiesManager();

        Player p1 = new Player(new Vector2d(0,  0));
        Player p2 = new Player(new Vector2d(10, 10)); // chevauchement (shapes 25×25)

        em.addEntity(p1);
        em.addEntity(p2);

        var collisions = p1.collisionsManager
                .findCollisionsWithData(em, new com.superkiment.common.blocks.BlocksManager());

        assertFalse(collisions.isEmpty(),
                "Deux joueurs qui se chevauchent doivent générer une collision");
    }

    @Test
    void findCollisions_twoFarPlayers_returnsNoCollision() {
        EntitiesManager em = new EntitiesManager();

        Player p1 = new Player(new Vector2d(0,    0));
        Player p2 = new Player(new Vector2d(1000, 1000));

        em.addEntity(p1);
        em.addEntity(p2);

        var collisions = p1.collisionsManager
                .findCollisionsWithData(em, new com.superkiment.common.blocks.BlocksManager());

        assertTrue(collisions.isEmpty(), "Deux joueurs éloignés ne doivent pas être en collision");
    }

    // ─────────────────── MTV ──────────────────────────────────────────────

    @Test
    void calculateRectRectMTV_nonZero_whenColliding() {
        Entity a = entityWithRect(0, 0, 50, 50);
        Entity b = entityWithRect(25, 0, 50, 50);

        CollisionShape shapeA = a.collisionsManager.collisionShapes.get(0);
        CollisionShape shapeB = b.collisionsManager.collisionShapes.get(0);

        Vector2d mtv = shapeA.calculateRectRectMTV(shapeB);
        assertNotNull(mtv);
        assertTrue(mtv.length() > 0, "Le MTV doit être non-nul pour séparer deux rects en collision");
    }
}
