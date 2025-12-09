package com.superkiment.client.graphics;

import com.superkiment.common.blocks.Block;
import com.superkiment.common.entities.Entity;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Renderer {
    public Renderer() {
    }

    public void renderEntities(Map<String, Entity> entities, Entity localPlayer) {
        for (Entity entity : entities.values()) {
            boolean isLocal = entity.id.equals(localPlayer.id);

            glPushMatrix();
            glTranslated(entity.posLerp.x, entity.posLerp.y, 0);
            double angleInRad = entity.dirLookLerp.angle(new Vector2d(1, 0)) * 57.2957795131d;
            glRotated(angleInRad, 0, 0, -1);
            entity.shapeModel.renderModel();

            glPopMatrix();
        }
    }

    public void renderBlocks(ArrayList<Block> blocks) {
        for (Block block : blocks) {
            glPushMatrix();
            glTranslated(block.pos.x * 50, block.pos.y * 50, 0);
            block.shapeModel.renderModel();

            glPopMatrix();
        }
    }

    public void renderFloor() {
    }

    public long SetupWindow() {
        // Setup error callback

        long window;

        GLFWErrorCallback.createPrint(System.err).set();

        // Initialiser GLFW
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configuration de la fenêtre
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Créer la fenêtre
        window = glfwCreateWindow(800, 600, "Mon Jeu LWJGL", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup key callback
        glfwSetKeyCallback(window, (window2, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window2, true);
        });

        // Centrer la fenêtre
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        // Activer le contexte OpenGL
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable v-sync

        glfwShowWindow(window);

        GL.createCapabilities();

        // Setup projection orthographique
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, 800, 600, 0, -1, 1); // (left, right, bottom, top, near, far)
        glMatrixMode(GL_MODELVIEW);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        return window;
    }
}
