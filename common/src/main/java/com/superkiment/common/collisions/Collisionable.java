package com.superkiment.common.collisions;

import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;

/**
 * Une classe qui étend Collisionable se définit comme ouverte aux intéractions de type collision.
 */
public abstract class Collisionable {
    public Vector2d pos;
    public ShapeModel shapeModel;
    public CollisionsManager collisionsManager;
    public boolean dirtyPosition = false;

    /**
     * Retourne la position mondiale de l'objet
     */
    public abstract Vector2d getWorldPosition();

    /**
     * Retourne la direction/orientation de l'objet (vecteur normalisé)
     * Utilisé pour calculer la rotation des formes de collision
     */
    public Vector2d getDirection() {
        return new Vector2d(0, 1);
    }

    public abstract void onCollision(Collisionable other);
}