package com.superkiment.client.graphics;

import com.superkiment.client.graphics.ui.UIElement;
import com.superkiment.common.blocks.Block;
import com.superkiment.common.collisions.Collisionable;
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
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Renderer {
    private FontManager fontManager;

    public Renderer() {
    }

    /**
     * Initialise les polices après la création du contexte OpenGL
     */
    public void initializeFonts() {
        fontManager = new FontManager();
        fontManager.initialize();
    }

    /**
     * Récupère le gestionnaire de polices
     */
    public FontManager getFontManager() {
        return fontManager;
    }

    public void renderUI(List<UIElement> elements) {
        for (UIElement element : elements) {
            glPushMatrix();
            glTranslated(element.pos.x, element.pos.y, 0);
            renderModel(element.shapeModel);
            glPopMatrix();
        }
    }

    public void renderEntities(Map<String, Entity> entities, Entity localPlayer) {
        for (Entity entity : entities.values()) {
            boolean isLocal = entity.id.equals(localPlayer.id);

            entity.updateLerp();

            glPushMatrix();
            glTranslated(entity.posLerp.x, entity.posLerp.y, 0);

            renderUpdateAroundCollisionableUI(entity);

            double angleInRad = entity.dirLookLerp.angle(new Vector2d(1, 0)) * 57.2957795131d;
            glRotated(angleInRad, 0, 0, -1);
            renderModel(entity.shapeModel);

            glPopMatrix();
        }
    }

    public void renderUpdateAroundCollisionableUI(Collisionable collisionable) {
        for (ShapeModel shapeModel : collisionable.uiShapeModels) {
            if (shapeModel == null) continue;
            shapeModel.update(collisionable);
            renderModel(shapeModel);
        }

    }

    public void renderBlocks(List<Block> blocks) {
        for (Block block : blocks) {
            glPushMatrix();
            glTranslated(block.pos.x * 50, block.pos.y * 50, 0);
            renderUpdateAroundCollisionableUI(block);
            renderModel(block.shapeModel);

            glPopMatrix();
        }
    }

    public void renderModel(ShapeModel shapeModel) {
        glPushMatrix();
        for (Shape shape : shapeModel.shapes) {
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
        glColor3d(shape.color.x, shape.color.y, shape.color.z);

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
                glVertex2f(0, 0);

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

        // Rendu du texte si présent
        if (shape.hasText() && fontManager != null) {
            renderText(shape);
        }

        glPopMatrix();
    }

    /**
     * Rendu du texte avec la bitmap font
     */
    private void renderText(Shape shape) {
        // Utiliser la police spécifiée ou la police par défaut
        BitmapFont font = fontManager.getFont(shape.fontName);

        float scale = shape.fontSize / (float) font.getBaseFontSize();

        // Calculer la position pour centrer le texte
        float textWidth = font.getTextWidth(shape.text, scale);
        float x = -textWidth / 2;
        float y = -shape.fontSize / 2;

        font.drawText(
                shape.text,
                x,
                y,
                scale,
                (float) shape.textColor.x,
                (float) shape.textColor.y,
                (float) shape.textColor.z
        );
    }

    public void renderFloor() {
        glClearColor(0.06f, 0.6f, 0.3f, 1.0f);
    }

    public long SetupWindow() {
        long window;

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(800, 600, "Mon Jeu LWJGL", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetKeyCallback(window, (window2, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window2, true);
        });

        glfwSetFramebufferSizeCallback(window, (window2, width, height) -> {
            updateViewport(width, height);
        });

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

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        updateViewport(800, 600);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Initialiser les polices après la création du contexte OpenGL
        initializeFonts();

        return window;
    }

    private void updateViewport(int width, int height) {
        glViewport(0, 0, width, height);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        if (fontManager != null) {
            fontManager.cleanup();
        }
    }

    public static Vector2d GetCurrentWindowSize(long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            return new Vector2d(pWidth.get(0), pHeight.get(0));
        }
    }
}