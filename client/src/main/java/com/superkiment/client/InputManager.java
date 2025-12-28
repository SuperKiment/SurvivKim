package com.superkiment.client;

import com.superkiment.client.network.handles.BlockHandle;
import com.superkiment.client.network.handles.EntityHandle;
import com.superkiment.common.entities.Player;
import com.superkiment.common.entities.Projectile;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;

import static java.lang.Math.round;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Gestionnaire d'inputs pour clavier et souris
 */
public class InputManager {

    private static InputManager instance;

    // États des touches
    private final Set<Integer> keysPressed = new HashSet<>();
    private final Set<Integer> keysJustPressed = new HashSet<>();
    private final Set<Integer> keysJustReleased = new HashSet<>();

    // États des boutons de souris
    private final Set<Integer> mouseButtonsPressed = new HashSet<>();
    private final Set<Integer> mouseButtonsJustPressed = new HashSet<>();
    private final Set<Integer> mouseButtonsJustReleased = new HashSet<>();

    // Position de la souris
    private final Vector2f mousePosition = new Vector2f(0, 0);
    private final Vector2f mouseDelta = new Vector2f(0, 0);
    private final Vector2f lastMousePosition = new Vector2f(0, 0);

    // Scroll
    private float scrollDelta = 0;

    // Système d'actions
    private final Map<String, List<Integer>> actionBindings = new HashMap<>();
    private final Map<String, Consumer<Void>> actionCallbacks = new HashMap<>();
    private final Map<String, Runnable> actionOnPress = new HashMap<>();
    private final Map<String, Runnable> actionOnRelease = new HashMap<>();

    private InputManager() {
    }

    public static InputManager getInstance() {
        if (instance == null) {
            instance = new InputManager();
        }
        return instance;
    }

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
        input.bindAction("ajouter block", GLFW_KEY_B);
        input.bindAction("tirer", GLFW_KEY_E);

        input.bindAction("action principale", -GLFW_MOUSE_BUTTON_LEFT);
        input.bindAction("action secondaire", -GLFW_MOUSE_BUTTON_RIGHT);

        // Action continue (appelée tant que la touche est enfoncée)
        input.onAction("avancer", (v) -> {
            player.dirDepl.y = -1d;
            player.moveFromInput = true;
        });

        input.onAction("reculer", (v) -> {
            player.dirDepl.y = 1d;
            player.moveFromInput = true;
        });

        input.onAction("gauche", (v) -> {
            player.dirDepl.x = -1d;
            player.moveFromInput = true;
        });

        input.onAction("droite", (v) -> {
            player.dirDepl.x = 1d;
            player.moveFromInput = true;
        });

        input.onActionPress("tirer", () -> {
            Vector2d projPos = new Vector2d(player.pos.x, player.pos.y);
            Vector2d projDir = new Vector2d(player.dirLookLerp.x, player.dirLookLerp.y);

            projDir
                    .normalize()
                    .mul(30);
            projPos.add(projDir);
            projDir.normalize();

            Projectile projectile = new Projectile(projPos, projDir);
            projectile.addCollisionException(player);
            EntityHandle.createEntity(projectile);
        });

        input.onActionPress("ajouter block", () -> {
            int posX = (int) round(player.pos.x / 50);
            int posY = (int) round(player.pos.y / 50);
            BlockHandle.createBlock(new Vector2d(posX, posY));
        });

        input.onActionPress("action principale", () -> {
            System.out.println("test souris");
        });

        input.onActionRelease("avancer", () -> player.dirDepl.y = 0);
        input.onActionRelease("reculer", () -> player.dirDepl.y = 0);
        input.onActionRelease("gauche", () -> player.dirDepl.x = 0);
        input.onActionRelease("droite", () -> player.dirDepl.x = 0);

        input.onActionPress("quitter", () -> glfwSetWindowShouldClose(window, true));

        // Afficher les bindings
        input.printBindings();
    }

    /**
     * Initialiser les callbacks GLFW
     */
    public void init(long window) {
        // Callback clavier
        GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_PRESS) {
                keysPressed.add(key);
                keysJustPressed.add(key);
                triggerActionOnPress(key);
            } else if (action == GLFW.GLFW_RELEASE) {
                keysPressed.remove(key);
                keysJustReleased.add(key);
                triggerActionOnRelease(key);
            }
        });

        // Callback position souris
        GLFW.glfwSetCursorPosCallback(window, (win, xpos, ypos) ->
                mousePosition.set((float) xpos, (float) ypos)
        );

        // Callback boutons souris
        GLFW.glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (action == GLFW.GLFW_PRESS) {
                mouseButtonsPressed.add(button);
                mouseButtonsJustPressed.add(button);
                triggerActionOnPressMouseButton(button);
            } else if (action == GLFW.GLFW_RELEASE) {
                mouseButtonsPressed.remove(button);
                mouseButtonsJustReleased.add(button);
                triggerActionOnReleaseMouseButton(button);
            }
        });

        // Callback scroll
        GLFW.glfwSetScrollCallback(window, (win, xoffset, yoffset) ->
                scrollDelta = (float) yoffset
        );
    }

    /**
     * Appeler à chaque frame pour mettre à jour les états
     */
    public void update() {
        keysJustPressed.clear();
        keysJustReleased.clear();
        mouseButtonsJustPressed.clear();
        mouseButtonsJustReleased.clear();

        // Calculer le delta de la souris
        mouseDelta.set(
                mousePosition.x - lastMousePosition.x,
                mousePosition.y - lastMousePosition.y
        );
        lastMousePosition.set(mousePosition);

        scrollDelta = 0;

        // Mettre à jour les actions continues
        updateContinuousActions();
    }

// ========== SOURIS ==========

    /**
     * Obtenir la position de la souris
     */
    public Vector2f getMousePosition() {
        return new Vector2f(mousePosition);
    }

    /**
     * Obtenir le déplacement de la souris depuis la dernière frame
     */
    public Vector2f getMouseDelta() {
        return new Vector2f(mouseDelta);
    }

    /**
     * Obtenir le delta de scroll
     */
    public float getScrollDelta() {
        return scrollDelta;
    }

// ========== SYSTÈME D'ACTIONS ==========

    /**
     * Lier une ou plusieurs touches/boutons à une action
     * Pour les boutons de souris, utiliser des valeurs négatives (ex: -GLFW_MOUSE_BUTTON_LEFT)
     */
    public void bindAction(String actionName, int... keyCodes) {
        actionBindings.putIfAbsent(actionName, new ArrayList<>());
        for (int key : keyCodes) {
            actionBindings.get(actionName).add(key);
        }
    }

    /**
     * Définir une callback pour une action (appelée en continu tant que pressée)
     */
    public void onAction(String actionName, Consumer<Void> callback) {
        actionCallbacks.put(actionName, callback);
    }

    /**
     * Définir une callback pour quand l'action est pressée (une seule fois)
     */
    public void onActionPress(String actionName, Runnable callback) {
        actionOnPress.put(actionName, callback);
    }

    /**
     * Définir une callback pour quand l'action est relâchée
     */
    public void onActionRelease(String actionName, Runnable callback) {
        actionOnRelease.put(actionName, callback);
    }

    /**
     * Vérifier si une action est active
     */
    public boolean isActionActive(String actionName) {
        List<Integer> keys = actionBindings.get(actionName);
        if (keys == null) return false;

        for (int key : keys) {
            // Si la valeur est négative, c'est un bouton de souris
            if (key < 0) {
                if (mouseButtonsPressed.contains(-key)) {
                    return true;
                }
            } else {
                if (keysPressed.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Vérifier si une action vient d'être pressée
     */
    public boolean isActionJustPressed(String actionName) {
        List<Integer> keys = actionBindings.get(actionName);
        if (keys == null) return false;

        for (int key : keys) {
            if (key < 0) {
                if (mouseButtonsJustPressed.contains(-key)) {
                    return true;
                }
            } else {
                if (keysJustPressed.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Vérifier si une action vient d'être relâchée
     */
    public boolean isActionJustReleased(String actionName) {
        List<Integer> keys = actionBindings.get(actionName);
        if (keys == null) return false;

        for (int key : keys) {
            if (key < 0) {
                if (mouseButtonsJustReleased.contains(-key)) {
                    return true;
                }
            } else {
                if (keysJustReleased.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Obtenir la force de l'action (pour les mouvements directionnels)
     * Retourne -1, 0 ou 1.
     */
    public float getActionAxis(String negativeAction, String positiveAction) {
        float value = 0;
        if (isActionActive(negativeAction)) value -= 1;
        if (isActionActive(positiveAction)) value += 1;
        return value;
    }

    /**
     * Débinder une action
     */
    public void unbindAction(String actionName) {
        actionBindings.remove(actionName);
        actionCallbacks.remove(actionName);
        actionOnPress.remove(actionName);
        actionOnRelease.remove(actionName);
    }

    /**
     * Débinder toutes les actions
     */
    public void clearAllBindings() {
        actionBindings.clear();
        actionCallbacks.clear();
        actionOnPress.clear();
        actionOnRelease.clear();
    }

// ========== MÉTHODES INTERNES ==========

    private void updateContinuousActions() {
        for (Map.Entry<String, Consumer<Void>> entry : actionCallbacks.entrySet()) {
            if (isActionActive(entry.getKey())) {
                entry.getValue().accept(null);
            }
        }
    }

    private void triggerActionOnPress(int keyCode) {
        for (Map.Entry<String, List<Integer>> entry : actionBindings.entrySet()) {
            if (entry.getValue().contains(keyCode)) {
                Runnable callback = actionOnPress.get(entry.getKey());
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    private void triggerActionOnPressMouseButton(int button) {
        for (Map.Entry<String, List<Integer>> entry : actionBindings.entrySet()) {
            if (entry.getValue().contains(-button)) {
                Runnable callback = actionOnPress.get(entry.getKey());
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    private void triggerActionOnRelease(int keyCode) {
        for (Map.Entry<String, List<Integer>> entry : actionBindings.entrySet()) {
            if (entry.getValue().contains(keyCode)) {
                Runnable callback = actionOnRelease.get(entry.getKey());
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    private void triggerActionOnReleaseMouseButton(int button) {
        for (Map.Entry<String, List<Integer>> entry : actionBindings.entrySet()) {
            if (entry.getValue().contains(-button)) {
                Runnable callback = actionOnRelease.get(entry.getKey());
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

// ========== UTILITAIRES ==========

    /**
     * Obtenir une représentation textuelle d'un keycode ou bouton de souris
     */
    public static String getKeyName(int keyCode) {
        // Si c'est un bouton de souris (valeur négative)
        if (keyCode < 1) {
            int mouseButton = -keyCode;
            return switch (mouseButton) {
                case GLFW_MOUSE_BUTTON_LEFT -> "MOUSE_LEFT";
                case GLFW_MOUSE_BUTTON_RIGHT -> "MOUSE_RIGHT";
                case GLFW_MOUSE_BUTTON_MIDDLE -> "MOUSE_MIDDLE";
                case GLFW_MOUSE_BUTTON_4 -> "MOUSE_4";
                case GLFW_MOUSE_BUTTON_5 -> "MOUSE_5";
                case GLFW_MOUSE_BUTTON_6 -> "MOUSE_6";
                case GLFW_MOUSE_BUTTON_7 -> "MOUSE_7";
                case GLFW_MOUSE_BUTTON_8 -> "MOUSE_8";
                default -> "MOUSE_" + mouseButton;
            };
        }

        // Pour les touches du clavier
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        return name != null ? name.toUpperCase() : "KEY_" + keyCode;
    }

    /**
     * Afficher toutes les actions liées
     */
    public void printBindings() {
        System.out.println("=== Actions liées ===");
        for (Map.Entry<String, List<Integer>> entry : actionBindings.entrySet()) {
            System.out.print(entry.getKey() + ": ");
            for (int key : entry.getValue()) {
                System.out.print(getKeyName(key) + " ");
            }
            System.out.println();
        }
    }

}