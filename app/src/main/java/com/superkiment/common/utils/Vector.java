package com.superkiment.common.utils;

import org.joml.Vector2d;

public class Vector {
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

    public static Vector2d Lerp(Vector2d vect, Vector2d target, double amount) {
        return new Vector2d(
                vect.x + (target.x - vect.x) * amount,
                vect.y + (target.y - vect.y) * amount
        );
    }
}
