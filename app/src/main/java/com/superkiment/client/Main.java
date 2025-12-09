package com.superkiment.client;

import com.superkiment.client.graphics.Renderer;
import com.superkiment.client.network.GameClient;
import com.superkiment.client.network.PlayerHandle;
import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import com.superkiment.client.graphics.ReusableShape;

public class Main {

    private long window;
    private InputManager input;
    public static GameClient gameClient;
    private Renderer renderer;

    public static EntitiesManager entitiesManager = new EntitiesManager();
    public static BlocksManager blocksManager = new BlocksManager();

    public void run() {
        System.out.println("Hello LWJGL " + org.lwjgl.Version.getVersion() + "!");

        //Setup window
        renderer = new Renderer();
        window = renderer.SetupWindow();

        //Lancer le vrai jeu
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
        gameClient = GameClient.tryConnectToServer(window);

        //Setup inputs
        input = InputManager.getInstance();
        InputManager.setupInputs(window, input, gameClient.getLocalPlayer());

        input.onActionPress("connecter", () -> {
            if (gameClient == null || !gameClient.isConnected()) {
                gameClient = GameClient.tryConnectToServer(window);
            }
        });
        input.bindAction("connecter", GLFW_KEY_C);
    }

    private void loop() {
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            input.update();

            float dt = Time.getDeltaTime();

            // ========== GESTION DU RÉSEAU ==========
            if (gameClient != null && gameClient.isConnected()) {
                gameTick(dt);
            } else {
                // Message de déconnexion
                new ReusableShape(400, 300)
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

    private void gameTick(float deltaTime) {
        Entity localPlayer = gameClient.getLocalPlayer();

        for (Entity entity : entitiesManager.getEntities().values()) {
            entity.update();
        }

        // Envoyer la position au serveur (UDP) régulièrement
        GameClient.positionSendTimer += deltaTime;
        if (GameClient.positionSendTimer >= GameClient.POSITION_SEND_RATE) {
            PlayerHandle.sendPosition();
            GameClient.positionSendTimer = 0;
        }
        renderer.renderEntities(entitiesManager.getEntities(), localPlayer);
        renderer.renderBlocks(blocksManager.getBlocks());
    }

    public static void main(String[] args) {
        new Main().run();
    }
}