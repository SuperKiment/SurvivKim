package com.superkiment.common.collisions;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

/**
 * Une classe qui étend Collisionable se définit comme ouverte aux intéractions de type collision.
 */
public abstract class Collisionable {
    /**
     * La position mondiale ou relative du Collisionable. (coordonnées en int pour un block/position mondiale pour une entité)
     */
    public Vector2d pos;

    /**
     * Le modèle qui permet de connaître les formes et de render le Collisionable.
     */
    public ShapeModel shapeModel;

    public CollisionsManager collisionsManager;

    /**
     * Est-ce que le Collisionable a bougé depuis le dernier tick ?
     */
    public boolean dirtyPosition = false;

    /**
     * Est-ce que le Collisionable réagit aux collisions de manière physique ?
     * <p>True : réagira aux physiques</p>
     * <p>False : passera à travers tout</p>
     */
    public boolean doReactCollision = true;

    /**
     * Liste d'exceptions qui seront complètement ignorées par le Collisionable. (Ne veut pas dire que l'autre l'ignorera)
     */
    private final List<Collisionable> exceptionsCollisions = new ArrayList<>();

    /**
     * Retourne la position mondiale de l'objet.
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


    public void addCollisionException(Entity e) {
        exceptionsCollisions.add(e);
    }

    public boolean hasCollisionException(Collisionable c) {
        return exceptionsCollisions.contains(c);
    }

    public int numberOfCollisionExceptions() {
        return exceptionsCollisions.size();
    }

    public Collisionable getCollisionException(int i) {
        return exceptionsCollisions.get(i);
    }
}