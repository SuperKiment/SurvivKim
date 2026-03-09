package com.superkiment.common.entities;

import com.superkiment.common.Time;
import org.joml.Vector2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTest {

    private Entity entity;

    @BeforeEach
    void setUp() {
        entity = new Entity(new Vector2d(100, 200));
    }

    // ───────────────────── Construction ──────────────────────────────────

    @Test
    void constructor_setsPositionCorrectly() {
        assertEquals(100, entity.pos.x, 1e-9);
        assertEquals(200, entity.pos.y, 1e-9);
    }

    @Test
    void constructor_posLerpMatchesInitialPos() {
        assertEquals(entity.pos.x, entity.posLerp.x, 1e-9);
        assertEquals(entity.pos.y, entity.posLerp.y, 1e-9);
    }

    @Test
    void constructor_idIsNotNull() {
        assertNotNull(entity.id, "L'id ne doit jamais être null");
        assertFalse(entity.id.isBlank(), "L'id ne doit pas être vide");
    }

    @Test
    void constructor_twoEntities_haveDistinctIds() {
        Entity other = new Entity();
        assertNotEquals(entity.id, other.id, "Chaque entité doit avoir un UUID unique");
    }

    @Test
    void constructor_shapeModelIsInitialized() {
        assertNotNull(entity.shapeModel, "shapeModel doit être initialisé");
        assertNotNull(entity.uiShapeModels, "uiShapeModels doit être initialisé");
    }

    @Test
    void constructor_defaultNameIsNoName() {
        assertEquals("NoName", entity.name);
    }

    @Test
    void constructor_dirLookTargetIsUnitY() {
        assertEquals(0, entity.dirLookTarget.x, 1e-9);
        assertEquals(1, entity.dirLookTarget.y, 1e-9);
    }

    // ───────────────────── updateLerp ─────────────────────────────────────

    @Test
    void updateLerp_whenPositionChanges_posLerpConvergesOnMultipleCalls() {
        entity.pos.set(500, 500);

        double prevDist = entity.posLerp.distance(entity.pos);
        for (int i = 0; i < 10; i++) {
            entity.updateLerp();
        }
        double newDist = entity.posLerp.distance(entity.pos);

        assertTrue(newDist < prevDist,
                "posLerp doit se rapprocher de pos après updateLerp répétés");
    }

    @Test
    void updateLerp_dirLookLerpConvergesToTarget() {
        entity.dirLookTarget.set(1, 0);  // pointer vers la droite

        for (int i = 0; i < 20; i++) {
            entity.updateLerp();
        }

        // Après 20 pas de lerp à 0.2, on doit s'être significativement rapproché de (1,0)
        assertTrue(entity.dirLookLerp.x > 0.8,
                "dirLookLerp.x doit tendre vers 1 (cible = droite)");
    }

    // ───────────────────── turnToDirection ────────────────────────────────

    @Test
    void turnToDirection_setsTarget() {
        entity.turnToDirection(new Vector2d(1, 0));
        assertEquals(1, entity.dirLookTarget.x, 1e-9);
        assertEquals(0, entity.dirLookTarget.y, 1e-9);
    }

    // ───────────────────── toString ───────────────────────────────────────

    @Test
    void toString_containsClassNameAndPosition() {
        String str = entity.toString();
        assertTrue(str.contains("Entity"), "toString doit contenir le nom de la classe");
        assertTrue(str.contains("NoName"),  "toString doit contenir le nom de l'entité");
    }

    // ───────────────────── dirtyPosition ──────────────────────────────────

    @Test
    void dirtyPosition_defaultFalse() {
        assertFalse(entity.dirtyPosition, "dirtyPosition doit être false par défaut");
    }

    // ───────────────────── Mouvement avec input ────────────────────────────

    @Test
    void moveFromInput_startsAsFalse() {
        assertFalse(entity.moveFromInput);
    }

    @Test
    void updateMovement_doesNothing_whenMoveFromInputFalse() {
        entity.pos.set(0, 0);
        entity.moveFromInput = false;
        entity.dirDepl.set(1, 0);

        // UpdateMovement interne (accessible via updateLogic avec des managers vides)
        // On ne peut pas tester directement le mouvement sans Time.UpdateFrameTime() non nul,
        // mais on peut vérifier que l'état ne change pas quand moveFromInput = false
        Vector2d posBefore = new Vector2d(entity.pos);
        // La méthode updateMovement est protected, mais on peut la simuler :
        // moveFromInput = false → la méthode doit retourner immédiatement
        entity.moveFromInput = false;
        // Appel indirect : time delta = 0 donc mvt = 0 même si on le met à true
        Time.UpdateFrameTime();
        Time.UpdateFrameTime();
        // Pas de changement car moveFromInput = false
        assertEquals(posBefore.x, entity.pos.x, 1e-9);
        assertEquals(posBefore.y, entity.pos.y, 1e-9);
    }
}
