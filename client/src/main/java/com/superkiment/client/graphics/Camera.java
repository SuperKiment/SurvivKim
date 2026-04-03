package com.superkiment.client.graphics;

import com.superkiment.client.Main;
import com.superkiment.common.entities.Entity;
import org.joml.Vector2d;

public class Camera {
    private static Camera instance;

    private Entity targetEntity;
    private Vector2d pos;

    private float cameraSpeed = 0.1f;

    private Camera() {
        this.follow(Main.gameClient.getLocalPlayer());
        this.pos = new Vector2d(targetEntity.pos.x, targetEntity.pos.y);
    }

    public static Camera getInstance() {
        if (instance == null) instance = new Camera();
        return instance;
    }

    public void follow(Entity e) {
        this.targetEntity = e;
    }

    public Camera update() {
        Vector2d winSize = Renderer.GetCurrentWindowSize();

        //Bouger la cam en lerp
        Vector2d targetPos = new Vector2d(targetEntity.pos.x, targetEntity.pos.y);
        targetPos.sub(winSize.div(2));
        pos.x = pos.x + (targetPos.x - pos.x) * cameraSpeed;
        pos.y = pos.y + (targetPos.y - pos.y) * cameraSpeed;



        return this;
    }

    public void setCameraSpeed(float s) {
        if (cameraSpeed > 0 && cameraSpeed <= 1) this.cameraSpeed = s;
    }

    public Vector2d getPos() {
        return pos;
    }
}
