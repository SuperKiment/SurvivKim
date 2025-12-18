package com.superkiment.common.entities;

import com.superkiment.common.Time;
import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.collisions.CollisionData;
import com.superkiment.common.collisions.Collisionable;
import com.superkiment.common.shapes.ShapeModel;
import com.superkiment.common.collisions.CollisionsManager;
import org.joml.Vector2d;

import java.util.List;
import java.util.UUID;

import static com.superkiment.common.utils.Vector.Lerp;
import static com.superkiment.common.utils.Vector.LerpRotation;

public class Entity extends Collisionable {
    public Vector2d posLerp,
            dirLookTarget, dirLookLerp, dirDepl;
    public float speed = 200f;
    public String id;
    public String name = "NoName";

    public boolean moveFromInput = false;

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
    }

    /**
     * Update toute la logique d'une entité. Utilisé par le serveur, à part pour le localPlayer dans le Client.
     *
     * @param entitiesManager
     * @param blocksManager
     */
    public void updateLogic(EntitiesManager entitiesManager, BlocksManager blocksManager) {
        dirtyPosition = false;
        updateMovement();
        {
            List<CollisionData> collisions = collisionsManager.findCollisionsWithData(entitiesManager, blocksManager);
            collisionsManager.reactToCollisions(collisions);
        }
    }

    public void turnToDirection(Vector2d dir) {
        this.dirLookTarget.set(dir);
    }

    /**
     * Met à jour le mouvement en fonction de la vitesse et de la direction.
     */
    protected void updateMovement() {
        if (!moveFromInput) return;

        dirLookTarget.set(dirDepl.x, dirDepl.y);

        Vector2d mvt = new Vector2d(dirDepl.x, dirDepl.y);
        if (mvt.lengthSquared() > 0) {
            mvt.normalize().mul(speed).mul(Time.getDeltaTime());
            collisionsManager.velocity.set(mvt);
            pos.add(mvt);
            dirtyPosition = true;
        }

        moveFromInput = false;
    }

    /**
     * Met à jour les différents lerp pour fluidifier les mouvements : rotation et déplacement.
     */
    public void updateLerp() {
        dirLookLerp = LerpRotation(dirLookLerp, dirLookTarget, 0.2);
        posLerp = Lerp(posLerp, pos, 0.3);
    }

    //GETTERS ET SETTERS
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