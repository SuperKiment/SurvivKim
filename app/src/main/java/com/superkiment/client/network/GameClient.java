package com.superkiment.client.network;

import com.superkiment.client.Main;
import com.superkiment.client.network.handles.BlockHandle;
import com.superkiment.client.network.handles.EntityHandle;
import com.superkiment.client.network.handles.PlayerHandle;
import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Player;
import com.superkiment.common.packets.*;
import org.joml.Vector2d;

import java.util.*;

import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;

public class GameClient {

    public static final String SERVER_ADDRESS = "localhost"; // ou "127.0.0.1"
    public static final int TCP_PORT = 7777;
    public static final int UDP_PORT = 7778;

    public static float positionSendTimer = 0;
    public static final float POSITION_SEND_RATE = 1.0f / 30.0f; // 30 Hz

    private final String serverAddress;
    private final int tcpPort;
    private final int udpPort;

    private TCPClient tcpClient;
    private UDPClient udpClient;

    //Références
    private final EntitiesManager entitiesManager;
    private final BlocksManager blocksManager;

    private Player localPlayer;
    private String playerId;

    private boolean connected = false;

    public GameClient(String serverAddress, int tcpPort, int udpPort) {
        this.serverAddress = serverAddress;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.entitiesManager = Main.entitiesManager;
        this.blocksManager = Main.blocksManager;
    }

    /**
     * Se connecter au serveur
     */
    public boolean connect(String playerName) {
        try {
            // Générer un ID unique pour le joueur
            playerId = UUID.randomUUID().toString();

            // Connecter TCP
            tcpClient = new TCPClient(serverAddress, tcpPort, this);
            tcpClient.connect();

            // Connecter UDP
            udpClient = new UDPClient(serverAddress, udpPort, this);
            udpClient.connect();

            // Créer l'entité du joueur local
            localPlayer = new Player(new Vector2d(400, 300));
            localPlayer.id = playerId;
            localPlayer.name = playerName;

            // Envoyer le packet de connexion (TCP)
            PacketPlayerJoin joinPacket = new PacketPlayerJoin(playerId, playerName);
            tcpClient.send(joinPacket);

            // Créer l'entité du joueur (TCP)
            PacketCreateEntity createPacket = new PacketCreateEntity(
                    playerId, playerName, localPlayer.pos
            );
            tcpClient.send(createPacket);

            // Ajouter le joueur local aux entités
            entitiesManager.getEntities().put(playerId, localPlayer);

            connected = true;
            System.out.println("Connecté au serveur en tant que " + playerName);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static GameClient tryConnectToServer(long window) {
        System.out.println("Tentative de connexion au serveur...");
        GameClient gameClient = new GameClient(SERVER_ADDRESS, TCP_PORT, UDP_PORT);

        boolean success = gameClient.connect("Player_" + System.currentTimeMillis() % 1000);

        if (success) {
            System.out.println("✓ Connecté au serveur !");
            glfwSetWindowTitle(window, "Mon Jeu Multijoueur - Connecté");
            return gameClient;
        }

        System.out.println("✗ Échec de connexion au serveur");
        glfwSetWindowTitle(window, "Mon Jeu Multijoueur - Déconnecté");
        return null;
    }


    /**
     * Gérer les packets TCP reçus
     */
    public void handleTCPPacket(Packet packet) {
        switch (packet.getType()) {
            case CREATE_ENTITY:
                EntityHandle.handleCreateEntity((PacketCreateEntity) packet);
                break;

            case DELETE_ENTITY:
                EntityHandle.handleDeleteEntity((PacketDeleteEntity) packet);
                break;

            case PLAYER_JOIN:
                PlayerHandle.handlePlayerJoin((PacketPlayerJoin) packet);
                break;

            case CREATE_BLOCK:
                BlockHandle.handleCreateBlock((PacketCreateBlock) packet);
                break;

            default:
                System.out.println("Type de packet TCP non géré: " + packet.getType());
        }
    }

    /**
     * Gérer les packets UDP reçus
     */
    public void handleUDPPacket(PacketEntityPosition packet) {
        // Ne pas mettre à jour notre propre position
        if (packet.entityId.equals(playerId)) {
            return;
        }

        Entity entity = entitiesManager.getEntities().get(packet.entityId);
        if (entity != null) {
            entity.pos.set(packet.posX, packet.posY);
            entity.dirLookTarget.set(packet.dirX, packet.dirY);
        }
    }

    /**
     * Déconnecter du serveur
     */
    public void disconnect() {
        connected = false;
        if (tcpClient != null) {
            tcpClient.disconnect();
        }
        if (udpClient != null) {
            udpClient.disconnect();
        }
    }

    // Getters
    public Player getLocalPlayer() {
        return localPlayer;
    }

    public boolean isConnected() {
        return connected;
    }

    public TCPClient getTCPClient() {
        return tcpClient;
    }

    public UDPClient getUDPClient() {
        return udpClient;
    }
}