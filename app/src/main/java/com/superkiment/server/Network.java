package com.superkiment.server;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.*;
import com.superkiment.server.monitor.ServerMonitor;
import org.joml.Vector2d;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

public class Network {

    private static Map<String, Entity> entities = null;
    private static Map<String, ClientConnection> clients = null;
    private static ServerMonitor monitor;

    public static void setupNetwork(Map<String, Entity> ents, Map<String, ClientConnection> clis) {
        entities = ents;
        clients = clis;
        monitor = ServerMonitor.getInstance();
    }

    /**
     * Gérer les packets TCP reçus
     */
    public static void handleTCPPacket(Packet packet, ClientConnection client) {
        System.out.println("TCP reçu: " + packet);

        try {
            monitor.logTCPReceived(packet);
        } catch (IOException ignored) {
        }

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
    public static void handleUDPPacket(PacketEntityPosition packet, InetAddress address, int port, UDPServer udpServer) {
        monitor.logUDPReceived(packet);

        ClientConnection client = clients.get(packet.entityId);
        if (client != null) {
            if (client.getUdpPort() == 0) {
                client.setUdpPort(port);
            }
        }

        Entity entity = entities.get(packet.entityId);
        if (entity != null) {
            entity.pos.set(packet.posX, packet.posY);
            entity.dirLook.set(packet.dirX, packet.dirY);

            broadcastPositionUDP(packet, address, port, udpServer);
        }
    }

    public static void handleCreateEntity(PacketCreateEntity packet, ClientConnection client) {
        Entity entity = new Entity(new Vector2d(packet.posX, packet.posY));
        entity.id = packet.entityId;
        entity.name = packet.entityName;

        entities.put(entity.id, entity);

        // Broadcaster à tous les clients
        broadcastTCP(packet, client);
        monitor.log("INFO", "Entité créée: " + entity.id + " (" + entity.name + ")");
    }

    public static void handleDeleteEntity(PacketDeleteEntity packet) {
        entities.remove(packet.entityId);
        System.out.println("Entité supprimée: " + packet.entityId);

        // Broadcaster à tous les clients
        broadcastTCP(packet, null);
        monitor.log("INFO", "Entité supprimée: " + packet.entityId);
    }

    public static void handlePlayerJoin(PacketPlayerJoin packet, ClientConnection client) {
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
        monitor.log("INFO", "Joueur a rejoint : " + client.playerId + " (" + client.playerName + ")");
    }

    /**
     * Envoyer un packet TCP à tous les clients sauf l'expéditeur
     */
    public static void broadcastTCP(Packet packet, ClientConnection except) {
        try {
            monitor.logTCPSent(packet);
        } catch (IOException ignored) {
        }

        for (ClientConnection client : clients.values()) {
            if (client != except) {
                client.sendTCP(packet);
            }
        }
    }

    /**
     * Envoyer une position UDP à tous les clients sauf l'expéditeur
     */
    public static void broadcastPositionUDP(PacketEntityPosition packet, InetAddress exceptAddress, int exceptPort, UDPServer udpServer) {
        monitor.logUDPSent(packet);

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
}
