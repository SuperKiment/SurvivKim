package com.superkiment.client;

import com.superkiment.client.graphics.Renderer;
import com.superkiment.client.graphics.ui.UIManager;
import com.superkiment.client.network.GameClient;
import com.superkiment.client.network.handles.PlayerHandle;
import com.superkiment.common.Time;
import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.EntityFactory;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Main {

    private static long window;
    private InputManager input;
    public static GameClient gameClient;

    private static final Renderer renderer = new Renderer();
    public static final UIManager uiManager = new UIManager();
    public static final EntitiesManager entitiesManager = new EntitiesManager();
    public static final BlocksManager blocksManager = new BlocksManager();

    public void run() {
        System.out.println("Hello LWJGL " + org.lwjgl.Version.getVersion() + "!");

        //Setup window
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
        //Setup et lancer les services
        uiManager.setup(window);

        EntityFactory.CreateInstance(entitiesManager);
        input = InputManager.getInstance();
        input.init(window);
        InputManager.setupGeneralInputs(input);

        input.onActionPress("connecter", Main::connect);
        input.bindAction("connecter", GLFW_KEY_C);
    }

    private void loop() {

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            input.update();

            float dt = Time.getDeltaTime();

            // ========== GESTION DU RÉSEAU ==========
            if (gameClient != null && gameClient.isConnected()) {
                gameTick(dt);
            }

            renderer.renderUpdateUI(uiManager.getUIElementsSortedByZ());

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

        localPlayer.updateLogic(entitiesManager, blocksManager);

        // Envoyer la position au serveur (UDP) régulièrement
        GameClient.positionSendTimer += deltaTime;
        if (GameClient.positionSendTimer >= GameClient.POSITION_SEND_RATE) {
            PlayerHandle.sendPosition();
            GameClient.positionSendTimer = 0;
        }
        renderer.renderFloor();
        renderer.renderEntities(entitiesManager.getEntities(), localPlayer);
        renderer.renderBlocks(blocksManager.getBlocks());
    }

    public static void main(String[] args) {
        new Main().run();
    }

    public static boolean connect() {
        if (gameClient == null || !gameClient.isConnected()) {
            GameClient gc = GameClient.tryConnectToServer(window);

            if (gc != null) {
                gameClient = gc;
                InputManager.setupGameInputs(window, InputManager.getInstance(), gameClient.getLocalPlayer());
                return true;
            }
        }

        return false;
    }
}