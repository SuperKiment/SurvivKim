package com.superkiment.common.utils;

import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorUtilsTest {

    private static final double DELTA = 1e-9;

    // ──────────────────────────── Lerp ────────────────────────────────────

    @Test
    void lerp_amountZero_returnsOriginalVector() {
        Vector2d from   = new Vector2d(10, 20);
        Vector2d target = new Vector2d(100, 200);
        Vector2d result = Vector.Lerp(from, target, 0.0);

        assertEquals(10,  result.x, DELTA, "x doit rester à 10 avec amount=0");
        assertEquals(20,  result.y, DELTA, "y doit rester à 20 avec amount=0");
    }

    @Test
    void lerp_amountOne_returnsTargetVector() {
        Vector2d from   = new Vector2d(0, 0);
        Vector2d target = new Vector2d(50, 80);
        Vector2d result = Vector.Lerp(from, target, 1.0);

        assertEquals(50, result.x, DELTA, "x doit atteindre la cible avec amount=1");
        assertEquals(80, result.y, DELTA, "y doit atteindre la cible avec amount=1");
    }

    @Test
    void lerp_amountHalf_returnsMidpoint() {
        Vector2d from   = new Vector2d(0, 0);
        Vector2d target = new Vector2d(10, 10);
        Vector2d result = Vector.Lerp(from, target, 0.5);

        assertEquals(5.0, result.x, DELTA, "Le milieu de 0→10 est 5");
        assertEquals(5.0, result.y, DELTA, "Le milieu de 0→10 est 5");
    }

    @Test
    void lerp_doesNotMutateInputVectors() {
        Vector2d from   = new Vector2d(1, 2);
        Vector2d target = new Vector2d(3, 4);
        Vector.Lerp(from, target, 0.5);

        assertEquals(1, from.x, DELTA,   "from.x ne doit pas être muté");
        assertEquals(2, from.y, DELTA,   "from.y ne doit pas être muté");
        assertEquals(3, target.x, DELTA, "target.x ne doit pas être muté");
        assertEquals(4, target.y, DELTA, "target.y ne doit pas être muté");
    }

    // ─────────────────────── LerpRotation ─────────────────────────────────

    @Test
    void lerpRotation_amountZero_returnsOriginalDirection() {
        Vector2d from   = new Vector2d(1, 0);
        Vector2d target = new Vector2d(0, 1);
        Vector2d result = Vector.LerpRotation(from, target, 0.0);

        // Avec amount=0 le résultat doit être identique à from
        assertEquals(from.x, result.x, 1e-6, "x doit rester le même avec amount=0");
        assertEquals(from.y, result.y, 1e-6, "y doit rester le même avec amount=0");
    }

    @Test
    void lerpRotation_preservesMagnitude() {
        // LerpRotation normalise l'axe de rotation mais pas le vecteur source :
        // la magnitude n'est conservée que si le vecteur source est lui-même unitaire.
        Vector2d from   = new Vector2d(1, 0);   // magnitude = 1 (vecteur unitaire)
        Vector2d target = new Vector2d(0, 1);
        Vector2d result = Vector.LerpRotation(from, target, 0.5);

        assertEquals(1.0, result.length(), 1e-6, "La magnitude doit être conservée pour un vecteur unitaire");
    }

    @Test
    void lerpRotation_rotatesInCorrectDirection() {
        Vector2d from   = new Vector2d(1, 0);  // angle = 0°
        Vector2d target = new Vector2d(0, 1);  // angle = 90°
        Vector2d result = Vector.LerpRotation(from, target, 1.0);

        // Avec amount=1 (full step = 90°) le résultat devrait approcher (0,1)
        assertTrue(result.y > result.x, "Après rotation de 90°, y doit être > x");
    }
}