package com.superkiment.client.graphics;

import com.superkiment.common.blocks.Block;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.shapes.Shape;
import com.superkiment.common.shapes.ShapeModel;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;

import static com.superkiment.common.shapes.Shape.ShapeType;
import static com.superkiment.common.shapes.Shape.color;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * The Renderer for the window ; all rendering is launched here
 */
public class Renderer {
    public Renderer() {
    }

    /**
     * Renders the entities on the window using OpenGL
     *
     * @param entities
     * @param localPlayer
     */
    public void renderEntities(Map<String, Entity> entities, Entity localPlayer) {
        for (Entity entity : entities.values()) {
            boolean isLocal = entity.id.equals(localPlayer.id);

            entity.updateLerp();

            glPushMatrix();
            glTranslated(entity.posLerp.x, entity.posLerp.y, 0);
            double angleInRad = entity.dirLookLerp.angle(new Vector2d(1, 0)) * 57.2957795131d;
            glRotated(angleInRad, 0, 0, -1);
            renderModel(entity.shapeModel);

            glPopMatrix();
        }
    }

    /**
     * Renders the blocks on the window using OpenGL
     *
     * @param blocks
     */
    public void renderBlocks(List<Block> blocks) {
        for (Block block : blocks) {
            glPushMatrix();
            glTranslated(block.pos.x * 50, block.pos.y * 50, 0);
            renderModel(block.shapeModel);

            glPopMatrix();
        }
    }

    public void renderModel(ShapeModel shapeModel) {
        glPushMatrix();
        for (Shape shape : shapeModel.shapes) {
            //Render all shapes
            renderShape(shape);
        }
        glPopMatrix();
    }

    public void renderShape(Shape shape) {
        Vector2d position = shape.position;
        Vector2d dimensions = shape.dimensions;
        int segments = shape.segments;
        float lineWidth = shape.lineWidth;
        ShapeType shapeType = shape.shapeType;

        glPushMatrix();
        glTranslated(position.x, position.y, 0);
        glColor3d(color.x, color.y, color.z);

        switch (shapeType) {
            case ShapeType.RECT -> {
                glBegin(GL_QUADS);
                glVertex2f(-(float) dimensions.x / 2, -(float) dimensions.y / 2);
                glVertex2f((float) dimensions.x / 2, -(float) dimensions.y / 2);
                glVertex2f((float) dimensions.x / 2, (float) dimensions.y / 2);
                glVertex2f(-(float) dimensions.x / 2, (float) dimensions.y / 2);
                glEnd();
            }
            case ShapeType.CIRCLE -> {
                glBegin(GL_TRIANGLE_FAN);
                glVertex2f(0, 0); // Centre

                for (int i = 0; i <= segments; i++) {
                    float angle = (float) (2.0 * Math.PI * i / segments);
                    double x = Math.cos(angle) * dimensions.x;
                    double y = Math.sin(angle) * dimensions.x;
                    glVertex2d(x, y);
                }
                glEnd();
            }
            case ShapeType.TRIANGLE -> {
            }
            case ShapeType.RECT_OUTLINE -> {
                glLineWidth(lineWidth);
                glColor3d(0, 0, 0);

                glBegin(GL_LINE_LOOP);
                glVertex2d(-dimensions.x / 2, -dimensions.y / 2);
                glVertex2d(dimensions.x / 2, -dimensions.y / 2);
                glVertex2d(dimensions.x / 2, dimensions.y / 2);
                glVertex2d(-dimensions.x / 2, dimensions.y / 2);
                glEnd();

                glPopMatrix();
            }
            case ShapeType.CIRCLE_OUTLINE -> {
                glLineWidth(lineWidth);
                glColor3d(0, 0, 0);

                glBegin(GL_LINE_LOOP);
                for (int i = 0; i < segments; i++) {
                    double angle = 2.0 * Math.PI * i / segments;
                    double x = Math.cos(angle) * dimensions.x;
                    double y = Math.sin(angle) * dimensions.x;
                    glVertex2d(x, y);
                }
                glEnd();
            }
            default -> throw new IllegalStateException("Unexpected value: " + shapeType);
        }
        glPopMatrix();
    }

    public void renderFloor() {
    }

    /**
     *
     * @return a fully setup window as long
     */
    public long SetupWindow() {
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
