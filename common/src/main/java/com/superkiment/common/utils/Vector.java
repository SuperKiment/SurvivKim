package com.superkiment.common.utils;

import org.joml.Vector2d;

public abstract class Vector {
    /**
     * Permet de lerp de Vector2d à Vector2d mais en gardant la magnitude, uniquement sur l'angle de rotation.
     *
     * @param vect   le vecteur à lerp
     * @param target le vecteur cible
     * @param amount le taux de lerp (0-1)
     * @return le vecteur à lerp avec le taux de lerp appliqué.
     */
    public static Vector2d LerpRotation(Vector2d vect, Vector2d target, double amount) {
        double angle = vect.angle(target);
        double maxStep = angle * amount;

        Vector2d axis = new Vector2d(
                -vect.y,
                vect.x
        ).normalize();

        double cos = Math.cos(maxStep);
        double sin = Math.sin(maxStep);

        return new Vector2d(
                vect.x * cos + axis.x * sin,
                vect.y * cos + axis.y * sin
        );
    }

    /**
     * Fonction de lerp pour le Vector2d.
     *
     * @param vect   le vecteur à lerp
     * @param target le vecteur cible
     * @param amount le taux de lerp (0-1)
     * @return le vecteur à lerp avec le taux de lerp appliqué.
     */
    public static Vector2d Lerp(Vector2d vect, Vector2d target, double amount) {
        return new Vector2d(
                vect.x + (target.x - vect.x) * amount,
                vect.y + (target.y - vect.y) * amount
        );
    }
}
