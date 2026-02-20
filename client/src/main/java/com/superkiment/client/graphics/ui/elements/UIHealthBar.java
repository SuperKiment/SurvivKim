package com.superkiment.client.graphics.ui.elements;

import com.superkiment.client.Main;
import com.superkiment.client.graphics.ui.UIElement;
import com.superkiment.common.entities.Player;
import com.superkiment.common.shapes.Shape;
import org.joml.Vector2d;
import org.joml.Vector3d;

public class UIHealthBar extends UIElement {
    int barInitWidth = 290;
    private float invMaxHP = 0;
    Player player;
    Shape bar;

    /**
     *
     * @param pos
     * @param z   doit être positif
     */
    public UIHealthBar(Vector2d pos, int z) {
        super(pos, z);

        if (Main.gameClient != null)
            player = Main.gameClient.getLocalPlayer();

        shapeModel.addShape(new Shape(new Vector2d(0, 0), new Vector2d(300, 30), Shape.ShapeType.RECT, new Vector3d(0, 0, 0)));

        bar = new Shape(new Vector2d(0, 0), new Vector2d(barInitWidth, 20), Shape.ShapeType.RECT, new Vector3d(1, 0, 0));

        shapeModel.addShape(bar);
    }

    @Override
    public void update() {
        if (Main.gameClient == null) return;

        if (player == null) {
            player = Main.gameClient.getLocalPlayer();
            invMaxHP = 1f / player.maxHP;
        } else {

            bar.dimensions.x = barInitWidth * player.hp * invMaxHP;
            bar.position.x = -(barInitWidth - bar.dimensions.x) * 0.5;

            if (bar.dimensions.x <= 0) {
                bar.position.x = 0;
                bar.dimensions.x = 0;
            }
        }
    }
}
