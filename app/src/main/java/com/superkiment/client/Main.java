package com.superkiment.client;

import com.superkiment.common.entities.Player;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static com.superkiment.client.Setup.setupInputs;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import com.superkiment.client.graphics.Shape;

public class Main {

    private long window;
    private InputManager input;

    private Player player;

    public void run() {
        System.out.println("Hello LWJGL " + org.lwjgl.Version.getVersion() + "!");

        init();
        loop();

        // Libérer les ressources
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup error callback
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
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true);
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

        player = new Player();

        input = InputManager.getInstance();
        setupInputs(window, input, player);
    }

    private void loop() {
        glClearColor(0.9f, 0.9f, 0.9f, 1.0f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            input.update();


            // Dessiner le joueur (carré rouge)
            new Shape((float) player.pos.x, (float) player.pos.y)
                    .setColor(1.0f, 0.3f, 0.3f)
                    .drawRect(40, 40);

            // Indicateur de sprint (cercle vert si shift pressé)
            if (input.isActionActive("sprint")) {
                new Shape((float) player.pos.x, (float) player.pos.y)
                        .setColor(0.3f, 1.0f, 0.3f)
                        .drawCircle(25, 16);
            }

            // Alternative : utiliser directement les touches
            if (input.isKeyJustPressed(GLFW_KEY_R)) {
                player.pos.x = 400d;
                player.pos.y = 300d;
                System.out.println("Position réinitialisée !");
            }

            // Utiliser la souris
            if (input.isMouseButtonJustPressed(GLFW_MOUSE_BUTTON_LEFT)) {
                var mousePos = input.getMousePosition();
                System.out.println("Clic à: " + mousePos.x + ", " + mousePos.y);

                // Dessiner un cercle bleu à la position du clic
                new Shape(mousePos.x, mousePos.y)
                        .setColor(0.3f, 0.5f, 1.0f)
                        .drawCircle(10, 16);
            }

            // Afficher la position de la souris en temps réel
            var mousePos = input.getMousePosition();
            new Shape(mousePos.x, mousePos.y)
                    .setColor(1.0f, 1.0f, 0.3f)
                    .drawCircle(5, 8);

            // Utiliser le scroll
            float scroll = input.getScrollDelta();
            if (scroll != 0) {
                System.out.println("Scroll: " + scroll);
            }

            // EXEMPLE D'UTILISATION DES FORMES

            // Rectangle rouge
            new Shape(200, 150)
                    .setColor(1.0f, 0.2f, 0.2f)
                    .drawRect(100, 80);

            // Cercle bleu
            new Shape(400, 150)
                    .setColor(0.2f, 0.5f, 1.0f)
                    .drawCircle(50, 32);

            // Triangle vert avec rotation
            new Shape(600, 150)
                    .setColor(0.2f, 1.0f, 0.3f)
                    .setRotation(45)
                    .drawTriangle(80);

            // Rectangle avec contour
            new Shape(200, 350)
                    .setColor(1.0f, 0.8f, 0.2f)
                    .drawRectOutline(120, 60, 3);

            // Cercle avec contour
            new Shape(400, 350)
                    .setColor(0.8f, 0.2f, 1.0f)
                    .drawCircleOutline(40, 32, 2);

            // Triangle avec contour et rotation animée
            new Shape(600, 350)
                    .setColor(1.0f, 0.5f, 0.2f)
                    .setRotation((float) (glfwGetTime() * 50)) // Rotation animée
                    .drawTriangleOutline(70, 2);

            glfwSwapBuffers(window);
            glfwPollEvents();
            Time.updateDeltaTime();
        }
    }

    public static void main(String[] args) {
        new Main().run();
    }




}