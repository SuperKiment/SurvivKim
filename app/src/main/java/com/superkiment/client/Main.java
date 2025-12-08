package com.superkiment.client;

import com.superkiment.client.graphics.Renderer;
import com.superkiment.client.network.GameClient;
import com.superkiment.common.Entity;
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
    private GameClient gameClient;
    private Renderer renderer;

    private Player player;

    public void run() {
        System.out.println("Hello LWJGL " + org.lwjgl.Version.getVersion() + "!");

        init();
        loop();

        if (gameClient != null) {
            gameClient.disconnect();
        }

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

        renderer = new Renderer();

        player = new Player();

        input = InputManager.getInstance();
        setupInputs(window, input, player);

        input.onActionPress("connecter", () -> {
            if (gameClient == null || !gameClient.isConnected()) {
                connectToServer();
            }
        });
        input.bindAction("connecter", GLFW_KEY_C);

        connectToServer();
    }

    private void loop() {
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            input.update();

            float dt = Time.getDeltaTime();

            // ========== GESTION DU RÉSEAU ==========
            if (gameClient != null && gameClient.isConnected()) {
                Entity localPlayer = gameClient.getLocalPlayer();

                // Déplacer le joueur local
                float speed = localPlayer.speed * dt;

                if (input.isActionActive("avancer")) {
                    localPlayer.pos.y -= speed;
                }
                if (input.isActionActive("reculer")) {
                    localPlayer.pos.y += speed;
                }
                if (input.isActionActive("gauche")) {
                    localPlayer.pos.x -= speed;
                }
                if (input.isActionActive("droite")) {
                    localPlayer.pos.x += speed;
                }

                // Envoyer la position au serveur (UDP) régulièrement
                GameClient.positionSendTimer += dt;
                if (GameClient.positionSendTimer >= GameClient.POSITION_SEND_RATE) {
                    gameClient.sendPosition();
                    GameClient.positionSendTimer = 0;
                }

                renderer.renderEntities(gameClient.getEntities(), localPlayer);

            } else {
                // Message de déconnexion
                new Shape(400, 300)
                        .setColor(1.0f, 0.5f, 0.2f)
                        .drawRect(200, 50);
            }

            // Quitter avec ESC
            if (input.isActionJustPressed("quitter")) {
                glfwSetWindowShouldClose(window, true);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
            Time.updateDeltaTime();
        }
    }

    private void connectToServer() {
        System.out.println("Tentative de connexion au serveur...");
        gameClient = new GameClient(GameClient.SERVER_ADDRESS, GameClient.TCP_PORT, GameClient.UDP_PORT);

        boolean success = gameClient.connect("Player_" + System.currentTimeMillis() % 1000);

        if (success) {
            System.out.println("✓ Connecté au serveur !");
            glfwSetWindowTitle(window, "Mon Jeu Multijoueur - Connecté");
        } else {
            System.out.println("✗ Échec de connexion au serveur");
            glfwSetWindowTitle(window, "Mon Jeu Multijoueur - Déconnecté");
        }
    }

    public static void main(String[] args) {
        new Main().run();
    }




}