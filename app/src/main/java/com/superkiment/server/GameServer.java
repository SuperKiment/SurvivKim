package com.superkiment.server;

import com.superkiment.common.Entity;
import com.superkiment.common.packets.*;
import org.joml.Vector2d;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {

    private static final int TCP_PORT = 7777;
    private static final int UDP_PORT = 7778;
    private static final int TICK_RATE = 20; // 20 ticks par seconde

    private TCPServer tcpServer;
    private UDPServer udpServer;

    private final Map<String, Entity> entities = new ConcurrentHashMap<>();
    private final Map<String, ClientConnection> clients = new ConcurrentHashMap<>();

    private boolean running = false;

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }

    public void start() {
        System.out.println("=== Démarrage du serveur ===");
        System.out.println("TCP Port: " + TCP_PORT);
        System.out.println("UDP Port: " + UDP_PORT);

        running = true;

        // Démarrer le serveur TCP
        tcpServer = new TCPServer(TCP_PORT, this);
        new Thread(tcpServer::start).start();

        // Démarrer le serveur UDP
        udpServer = new UDPServer(UDP_PORT, this);
        new Thread(udpServer::start).start();

        // Boucle principale du serveur
        gameLoop();
    }

    private void gameLoop() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1_000_000_000.0 / TICK_RATE;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            if (delta >= 1) {
                tick();
                delta--;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void tick() {
        // Logique du serveur (update entités, etc.)
        // Pour l'instant, rien à faire ici
    }

    /**
     * Gérer les packets TCP reçus
     */
    public void handleTCPPacket(Packet packet, ClientConnection client) {
        System.out.println("TCP reçu: " + packet);

        switch (packet.getType()) {
            case CREATE_ENTITY:
                handleCreateEntity((PacketCreateEntity) packet, client);
                break;

            case DELETE_ENTITY:
                handleDeleteEntity((PacketDeleteEntity) packet);
                break;

            case PLAYER_JOIN:
                handlePlayerJoin((PacketPlayerJoin) packet, client);
                break;

            default:
                System.out.println("Type de packet TCP non géré: " + packet.getType());
        }
    }

    /**
     * Gérer les packets UDP reçus
     */
    public void handleUDPPacket(PacketEntityPosition packet, InetAddress address, int port) {
        ClientConnection client = clients.get(packet.entityId);
        if (client != null) {
            if (client.getUdpPort() == 0) {
                client.setUdpPort(port);
            }
        }

        Entity entity = entities.get(packet.entityId);
        if (entity != null) {
            entity.pos.set(packet.posX, packet.posY);
            entity.dir.set(packet.dirX, packet.dirY);

            broadcastPositionUDP(packet, address, port);
        }
    }

    private void handleCreateEntity(PacketCreateEntity packet, ClientConnection client) {
        Entity entity = new Entity(new Vector2d(packet.posX, packet.posY));
        entity.id = packet.entityId;
        entity.name = packet.entityName;

        entities.put(entity.id, entity);

        System.out.println("Entité créée: " + entity.id + " (" + entity.name + ")");

        // Broadcaster à tous les clients
        broadcastTCP(packet, client);
    }

    private void handleDeleteEntity(PacketDeleteEntity packet) {
        entities.remove(packet.entityId);
        System.out.println("Entité supprimée: " + packet.entityId);

        // Broadcaster à tous les clients
        broadcastTCP(packet, null);
    }

    private void handlePlayerJoin(PacketPlayerJoin packet, ClientConnection client) {
        client.playerId = packet.playerId;
        client.playerName = packet.playerName;
        clients.put(packet.playerId, client);

        System.out.println("Joueur connecté: " + packet.playerName + " (" + packet.playerId + ")");

        // Envoyer toutes les entités existantes au nouveau joueur
        for (Entity entity : entities.values()) {
            PacketCreateEntity createPacket = new PacketCreateEntity(
                    entity.id, entity.name, entity.pos
            );
            client.sendTCP(createPacket);
        }

        // Broadcaster le nouveau joueur aux autres
        broadcastTCP(packet, client);
    }

    /**
     * Envoyer un packet TCP à tous les clients sauf l'expéditeur
     */
    private void broadcastTCP(Packet packet, ClientConnection except) {
        for (ClientConnection client : clients.values()) {
            if (client != except) {
                client.sendTCP(packet);
            }
        }
    }

    /**
     * Envoyer une position UDP à tous les clients sauf l'expéditeur
     */
    private void broadcastPositionUDP(PacketEntityPosition packet, InetAddress exceptAddress, int exceptPort) {
        for (ClientConnection client : clients.values()) {
            int udpPort = client.getUdpPort();
            if (udpPort == 0) {
                continue;
            }
            if (client.getAddress().equals(exceptAddress) && udpPort == exceptPort) {
                continue;
            }
            udpServer.sendPosition(packet, client.getAddress(), udpPort);
        }
    }


    public void removeClient(ClientConnection client) {
        if (client.playerId != null) {
            clients.remove(client.playerId);
            System.out.println("Client déconnecté: " + client.playerName);

            // Supprimer l'entité du joueur
            if (entities.containsKey(client.playerId)) {
                PacketDeleteEntity packet = new PacketDeleteEntity(client.playerId);
                broadcastTCP(packet, null);
                entities.remove(client.playerId);
            }
        }
    }

    public void stop() {
        running = false;
        tcpServer.stop();
        udpServer.stop();
    }
}