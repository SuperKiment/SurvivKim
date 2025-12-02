package com.superkiment.client;

import com.superkiment.common.entities.Player;
import org.joml.Vector2d;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

public class Setup {

    public static void setupInputs(long window, InputManager input, Player player) {
        input.init(window);

        // Lier les touches aux actions
        input.bindAction("avancer", GLFW_KEY_W, GLFW_KEY_UP);
        input.bindAction("reculer", GLFW_KEY_S, GLFW_KEY_DOWN);
        input.bindAction("gauche", GLFW_KEY_A, GLFW_KEY_LEFT);
        input.bindAction("droite", GLFW_KEY_D, GLFW_KEY_RIGHT);
        input.bindAction("sauter", GLFW_KEY_SPACE);
        input.bindAction("quitter", GLFW_KEY_ESCAPE);
        input.bindAction("sprint", GLFW_KEY_LEFT_SHIFT);

        // Action continue (appelée tant que la touche est enfoncée)
        input.onAction("avancer", (v) -> {
            player.turnToDirection(new Vector2d(0, -1));
            player.moveInDirection();
        });

        input.onAction("reculer", (v) -> {
            player.turnToDirection(new Vector2d(0, 1));
            player.moveInDirection();
        });

        input.onAction("gauche", (v) -> {
            player.turnToDirection(new Vector2d(-1, 0));
            player.moveInDirection();
        });

        input.onAction("droite", (v) -> {
            player.turnToDirection(new Vector2d(1, 0));
            player.moveInDirection();
        });

        // Action au moment du press (une seule fois)
        //input.onActionPress("sauter", () -> {
        //  jumpCount++;
        // System.out.println("Saut ! Total: " + jumpCount);
        //});

        input.onActionPress("quitter", () -> {
            glfwSetWindowShouldClose(window, true);
        });

        // Afficher les bindings
        input.printBindings();
    }
}
