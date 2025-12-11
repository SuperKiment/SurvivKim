package com.superkiment.common.entities;

import com.superkiment.client.Time;
import com.superkiment.common.blocks.Block;
import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.collisions.CollisionData;
import com.superkiment.common.collisions.Collisionable;
import com.superkiment.client.graphics.Shape;
import com.superkiment.client.graphics.ShapeModel;
import com.superkiment.common.collisions.CollisionsManager;
import com.superkiment.common.collisions.CollisionShape;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.superkiment.common.collisions.CollisionShape.GetCollisionType;
import static com.superkiment.common.utils.Vector.Lerp;
import static com.superkiment.common.utils.Vector.LerpRotation;

public class Entity extends Collisionable {
    public Vector2d posLerp,
            dirLookTarget, dirLookLerp, dirDepl;
    public float speed = 200f;
    public String id;
    public String name = "NoName";

    public boolean moveFromInput = false;
    public boolean dirtyPosition = false;

    // Propriétés physiques
    public float mass = 1.0f; // Masse de l'entité
    public float friction = 0.8f; // Coefficient de friction (0-1)
    public float bounciness = 0.5f; // Rebond (0-1, 0 = pas de rebond)
    public Vector2d velocity = new Vector2d(0, 0); // Vélocité actuelle

    protected EntitiesManager entitiesManager;

    public Entity() {
        this(new Vector2d(200f, 200f));
    }

    public Entity(Vector2d pos) {
        this.id = UUID.randomUUID().toString();
        this.pos = new Vector2d(pos.x, pos.y);
        this.posLerp = new Vector2d(pos.x, pos.y);
        this.dirLookTarget = new Vector2d(0, 1);
        this.dirLookLerp = new Vector2d(0, 1);
        this.dirDepl = new Vector2d(0, 0);

        shapeModel = new ShapeModel();

        collisionsManager = new CollisionsManager(this);
        collisionsManager.addCollisionShape(
                new CollisionShape(
                        new Vector2d(0, 0),
                        new Vector2d(25, 25),
                        Shape.ShapeType.CIRCLE_OUTLINE,
                        this
                )
        );
    }

    public void updateLogic(EntitiesManager entitiesManager, BlocksManager blocksManager) {
        dirtyPosition = false;
        updateMovement();
        {
            List<CollisionData> collisions = findCollisionsWithData(entitiesManager, blocksManager);
            System.out.println(collisions.size());
            reactToCollisions(collisions);
        }
    }

    public void turnToDirection(Vector2d dir) {
        this.dirLookTarget.set(dir);
    }

    protected void updateMovement() {
        if (!moveFromInput) return;

        dirLookTarget.set(dirDepl.x, dirDepl.y);

        Vector2d mvt = new Vector2d(dirDepl.x, dirDepl.y);
        if (mvt.lengthSquared() > 0) {
            mvt.normalize().mul(speed).mul(Time.getDeltaTime());
            velocity.set(mvt);
            pos.add(mvt);
            dirtyPosition = true;
        }

        moveFromInput = false;
    }



    protected List<CollisionData> findCollisionsWithData(EntitiesManager entitiesManager,
                                                         BlocksManager blocksManager) {
        List<CollisionData> collisions = new ArrayList<>();

        // Collisions avec les entités
        for (Entity entity : entitiesManager.getEntities().values()) {
            if (entity == this) continue;

            for (CollisionShape thisShape : collisionsManager.collisionShapes) {
                for (CollisionShape otherShape : entity.collisionsManager.collisionShapes) {
                    if (thisShape.isInCollisionWith(otherShape)) {
                        String type = GetCollisionType(thisShape, otherShape);
                        collisions.add(new CollisionData(
                                entity.collisionsManager,
                                thisShape,
                                otherShape,
                                type
                        ));
                    }
                }
            }
        }

        // Collisions avec les blocs
        for (Block block : blocksManager.getBlocks()) {
            for (CollisionShape thisShape : collisionsManager.collisionShapes) {
                for (CollisionShape otherShape : block.collisionsManager.collisionShapes) {
                    if (thisShape.isInCollisionWith(otherShape)) {
                        String type = GetCollisionType(thisShape, otherShape);
                        collisions.add(new CollisionData(
                                block.collisionsManager,
                                thisShape,
                                otherShape,
                                type
                        ));
                    }
                }
            }
        }

        return collisions;
    }

    protected void reactToCollisions(List<CollisionData> collisions) {
        if (collisions.isEmpty()) return;

        for (CollisionData collision : collisions) {
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
        pos.add(mtv);

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

        dirtyPosition = true;
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
            otherVelocity.set(((Entity) collision.other.parent).velocity);
        }

        Vector2d relativeVelocity = new Vector2d(velocity).sub(otherVelocity);
        double velocityAlongNormal = relativeVelocity.dot(normal);

        // Ne résout que si les objets se rapprochent
        if (velocityAlongNormal < 0) {
            // Calcule l'impulsion
            float otherMass = 1.0f;
            if (collision.other.parent instanceof Entity) {
                otherMass = ((Entity) collision.other.parent).mass;
            } else {
                otherMass = Float.MAX_VALUE; // Objet immobile
            }

            float e = Math.min(bounciness,
                    collision.other.parent instanceof Entity ?
                            ((Entity) collision.other.parent).bounciness : 0);

            double j = -(1 + e) * velocityAlongNormal;
            j /= (1 / mass + 1 / otherMass);

            Vector2d impulse = new Vector2d(normal).mul(j);

            // Applique l'impulsion
            velocity.add(new Vector2d(impulse).mul(1 / mass));

            // Si l'autre objet est une entité, applique l'impulsion opposée
            if (collision.other.parent instanceof Entity) {
                Entity otherEntity = (Entity) collision.other.parent;
                otherEntity.velocity.sub(new Vector2d(impulse).mul(1 / otherMass));
                otherEntity.dirtyPosition = true;
            }
        }

        // Sépare les objets
        if (collision.other.parent instanceof Entity) {
            float totalMass = mass + ((Entity) collision.other.parent).mass;
            float ratio = mass / totalMass;
            pos.add(new Vector2d(mtv).mul(ratio));

            Entity otherEntity = (Entity) collision.other.parent;
            otherEntity.pos.sub(new Vector2d(mtv).mul(1 - ratio));
            otherEntity.dirtyPosition = true;
        } else {
            pos.add(mtv);
        }

        // Applique la friction tangentielle pour le glissement
        if (velocity.lengthSquared() > 0.01) {
            Vector2d tangent = new Vector2d(-normal.y, normal.x);
            double velocityAlongTangent = velocity.dot(tangent);

            Vector2d frictionForce = new Vector2d(tangent)
                    .mul(-velocityAlongTangent * friction * Time.getDeltaTime() * 50);
            velocity.add(frictionForce);
        }

        dirtyPosition = true;
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

        double distance = separation.length();
        if (distance < 0.001) {
            separation.set(1, 0); // Direction par défaut
            distance = 1;
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

            if (isThisCircle) {
                // Le cercle glisse
                pos.add(push);

                // Calcule la vélocité tangentielle
                double velocityAlongNormal = velocity.dot(normal);
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
                // Le rectangle s'arrête net
                pos.add(push);

                // Annule la vélocité dans la direction normale
                double velocityAlongNormal = velocity.dot(normal);
                if (velocityAlongNormal < 0) {
                    velocity.sub(new Vector2d(normal).mul(velocityAlongNormal));
                }
            }

            dirtyPosition = true;
        }
    }

    public void updateLerp() {
        dirLookLerp = LerpRotation(dirLookLerp, dirLookTarget, 0.2);
        posLerp = Lerp(posLerp, pos, 0.3);
    }

    public void setEntitiesManager(EntitiesManager entitiesManager) {
        this.entitiesManager = entitiesManager;
    }

    @Override
    public Vector2d getWorldPosition() {
        return pos;
    }

    @Override
    public Vector2d getDirection() {
        return new Vector2d(dirLookLerp).normalize();
    }
}