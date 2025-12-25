package com.superkiment.common.collisions;

import com.superkiment.common.Time;
import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

import static com.superkiment.common.collisions.CollisionShape.GetCollisionType;

/**
 * Manager de collisions pour un Collisionable. Cherche les collisions et applique les réactions. Collectionne également les CollectionShapes.
 */
public class CollisionsManager {
    public Collisionable parent;
    public List<CollisionShape> collisionShapes;

    // Propriétés physiques
    public float mass = 1.0f; // Masse de l'entité
    public float friction = 0.8f; // Coefficient de friction (0-1)
    public float bounciness = 0.5f; // Rebond (0-1, 0 = pas de rebond)
    public Vector2d velocity = new Vector2d(0, 0); // Vélocité actuelle

    private CollisionsManager() {
    }

    public CollisionsManager(Collisionable parent) {
        this.parent = parent;
        collisionShapes = new ArrayList<>();
    }

    public void addCollisionShape(CollisionShape collisionShape) {
        parent.shapeModel.addShape(collisionShape);
        collisionShapes.add(collisionShape);
    }

    public List<CollisionData> findCollisionsWithData(EntitiesManager entitiesManager,
                                                      BlocksManager blocksManager) {
        List<CollisionData> collisions = new ArrayList<>();
        List<Collisionable> testedCollisionables = new ArrayList<>();

        // Récupère les CollisionsManager
        for (Entity entity : entitiesManager.getEntities().values()) {
            if (entity == parent) continue;
            testedCollisionables.add(entity);
        }

        testedCollisionables.addAll(blocksManager.getBlocks());

        //Traite les CollisionsManager
        for (Collisionable otherCollisionable : testedCollisionables) {
            //Ignorer les exceptions
            if (parent.hasCollisionException(otherCollisionable)) continue;

            for (CollisionShape thisShape : parent.collisionsManager.collisionShapes) {
                for (CollisionShape otherShape : otherCollisionable.collisionsManager.collisionShapes) {
                    if (thisShape.isInCollisionWith(otherShape)) {
                        String type = GetCollisionType(thisShape, otherShape);
                        collisions.add(new CollisionData(
                                otherCollisionable.collisionsManager,
                                thisShape,
                                otherShape,
                                type
                        ));
                        parent.onCollision(otherCollisionable);
                    }
                }
            }
        }


        return collisions;
    }

    public void reactToCollisions(List<CollisionData> collisions) {
        if (collisions.isEmpty() || !parent.doReactCollision) return;

        for (CollisionData collision : collisions) {
            if (!collision.other.parent.doReactCollision) continue;

            switch (collision.collisionType) {
                case "RR" -> handleRectRectCollision(collision);
                case "CC" -> handleCircleCircleCollision(collision);
                case "RC" -> handleRectCircleCollision(collision, true);
                case "CR" -> handleRectCircleCollision(collision, false);
            }
        }
    }

    /**
     * Gestion collision Rectangle-Rectangle : Arrêt net, push-back
     */
    private void handleRectRectCollision(CollisionData collision) {
        Vector2d mtv = collision.thisShape.calculateRectRectMTV(collision.otherShape);

        // Applique le MTV complet pour séparer
        parent.pos.add(mtv);

        // Annule la vélocité dans la direction de la collision
        Vector2d normal = new Vector2d(mtv).normalize();
        double velocityAlongNormal = velocity.dot(normal);

        if (velocityAlongNormal < 0) {
            // Retire la composante de vélocité dans la direction de la collision
            velocity.sub(new Vector2d(normal).mul(velocityAlongNormal));

            // Applique un léger rebond si configuré
            if (bounciness > 0) {
                velocity.add(new Vector2d(normal).mul(-velocityAlongNormal * bounciness));
            }
        }

        parent.dirtyPosition = true;
    }

    /**
     * Gestion collision Cercle-Cercle : Glissement fluide avec physique
     */
    private void handleCircleCircleCollision(CollisionData collision) {
        Vector2d mtv = collision.thisShape.calculateCircleCircleMTV(collision.otherShape);
        Vector2d normal = new Vector2d(mtv).normalize();

        // Calcule les vélocités relatives
        Vector2d otherVelocity = new Vector2d(0, 0);
        if (collision.other.parent instanceof Entity) {
            otherVelocity.set((collision.other.parent).collisionsManager.velocity);
        }

        Vector2d relativeVelocity = new Vector2d(velocity).sub(otherVelocity);
        double velocityAlongNormal = relativeVelocity.dot(normal);

        // Ne résout que si les objets se rapprochent
        if (velocityAlongNormal < 0) {
            // Calcule l'impulsion
            float otherMass;
            if (collision.other.parent instanceof Entity) {
                otherMass = (collision.other.parent).collisionsManager.mass;
            } else {
                otherMass = Float.MAX_VALUE; // Objet immobile
            }

            float e = Math.min(bounciness,
                    collision.other.parent instanceof Entity ?
                            (collision.other.parent).collisionsManager.bounciness : 0);

            double j = -(1 + e) * velocityAlongNormal;
            j /= (1 / mass + 1 / otherMass);

            Vector2d impulse = new Vector2d(normal).mul(j);

            // Applique l'impulsion
            velocity.add(new Vector2d(impulse).mul(1 / mass));

            // Si l'autre objet est une entité, applique l'impulsion opposée
            if (collision.other.parent instanceof Entity otherEntity) {
                otherEntity.collisionsManager.velocity.sub(new Vector2d(impulse).mul(1 / otherMass));
                otherEntity.dirtyPosition = true;
            }
        }

        // Sépare les objets
        if (collision.other.parent instanceof Entity otherEntity) {
            float totalMass = mass + collision.other.parent.collisionsManager.mass;
            float ratio = mass / totalMass;
            parent.pos.add(new Vector2d(mtv).mul(ratio));

            otherEntity.pos.sub(new Vector2d(mtv).mul(1 - ratio));
            otherEntity.dirtyPosition = true;
        } else {
            parent.pos.add(mtv);
        }

        // Applique la friction tangentielle pour le glissement
        if (velocity.lengthSquared() > 0.01) {
            Vector2d tangent = new Vector2d(-normal.y, normal.x);
            double velocityAlongTangent = velocity.dot(tangent);

            Vector2d frictionForce = new Vector2d(tangent)
                    .mul(-velocityAlongTangent * friction * Time.getDeltaTime() * 50);
            velocity.add(frictionForce);
        }

        parent.dirtyPosition = true;
    }

    /**
     * Gestion collision Rectangle-Cercle : Comportement hybride
     */
    private void handleRectCircleCollision(CollisionData collision, boolean thisIsRect) {
        CollisionShape rect = thisIsRect ? collision.thisShape : collision.otherShape;
        CollisionShape circle = thisIsRect ? collision.otherShape : collision.thisShape;
        boolean isThisCircle = !thisIsRect;

        Vector2d circleCenter = circle.getWorldPosition();
        Vector2d rectCenter = rect.getWorldPosition();
        Vector2d separation = new Vector2d(circleCenter).sub(rectCenter);

        if (separation.length() < 0.001) {
            separation.set(1, 0); // Direction par défaut
        }

        Vector2d normal = new Vector2d(separation).normalize();

        // Calcule la pénétration
        double radius = circle.dimensions.x;
        Vector2d rectDir = rect.parent.getDirection();
        Vector2d rectPerp = new Vector2d(-rectDir.y, rectDir.x);

        Vector2d localCircle = new Vector2d(circleCenter).sub(rectCenter);
        double localX = localCircle.dot(rectDir);
        double localY = localCircle.dot(rectPerp);

        double halfWidth = rect.dimensions.x / 2;
        double halfHeight = rect.dimensions.y / 2;

        double closestX = Math.max(-halfWidth, Math.min(localX, halfWidth));
        double closestY = Math.max(-halfHeight, Math.min(localY, halfHeight));

        double dx = localX - closestX;
        double dy = localY - closestY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double penetration = radius - dist;

        if (penetration > 0) {
            Vector2d push = new Vector2d(normal).mul(penetration);

            parent.pos.add(push);
            double velocityAlongNormal = velocity.dot(normal);
            if (isThisCircle) {

                if (velocityAlongNormal < 0) {
                    Vector2d tangent = new Vector2d(-normal.y, normal.x);
                    double velocityAlongTangent = velocity.dot(tangent);

                    // Conserve le mouvement tangentiel avec friction
                    velocity.set(tangent.mul(velocityAlongTangent * (1 - friction * 0.3)));

                    // Ajoute un léger rebond si configuré
                    if (bounciness > 0) {
                        velocity.add(new Vector2d(normal).mul(-velocityAlongNormal * bounciness));
                    }
                }
            } else {
                if (velocityAlongNormal < 0) {
                    velocity.sub(new Vector2d(normal).mul(velocityAlongNormal));
                }
            }

            parent.dirtyPosition = true;
        }
    }
}
