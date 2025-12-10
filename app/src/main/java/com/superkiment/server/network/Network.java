package com.superkiment.server.network;

import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.*;
import com.superkiment.common.packets.entity.PacketCreateEntity;
import com.superkiment.common.packets.entity.PacketDeleteEntity;
import com.superkiment.common.packets.entity.PacketEntityPosition;
import com.superkiment.server.monitor.ServerMonitor;
import com.superkiment.server.network.handles.BlockHandle;
import com.superkiment.server.network.handles.EntityHandle;
import com.superkiment.server.network.handles.PlayerHandle;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Network {

    //Références
    private static ServerMonitor monitor;
    private static EntitiesManager entitiesManager;

    public static void setupNetwork(EntitiesManager em) {
        monitor = ServerMonitor.getInstance();
        entitiesManager = em;
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
                EntityHandle.handleCreateEntity((PacketCreateEntity) packet, client);
                break;

            case DELETE_ENTITY:
                EntityHandle.handleDeleteEntity((PacketDeleteEntity) packet);
                break;

            case PLAYER_JOIN:
                PlayerHandle.handlePlayerJoin((PacketPlayerJoin) packet, client);
                break;

            case CREATE_BLOCK:
                BlockHandle.handleCreateBlock((PacketCreateBlock) packet, client);
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

        ClientConnection client = entitiesManager.getClients().get(packet.entityId);
        if (client != null) {
            if (client.getUdpPort() == 0) {
                client.setUdpPort(port);
            }
        }

        Entity entity = entitiesManager.getEntities().get(packet.entityId);
        if (entity != null) {
            entity.pos.set(packet.posX, packet.posY);
            entity.dirLookTarget.set(packet.dirX, packet.dirY);

            broadcastPositionUDP(packet, address, port, udpServer);
        }
    }


    /**
     * Envoyer un packet TCP à tous les clients sauf l'expéditeur
     */
    public static void broadcastTCP(Packet packet, ClientConnection except) {
        try {
            monitor.logTCPSent(packet);
        } catch (IOException ignored) {
        }

        for (ClientConnection client : entitiesManager.getClients().values()) {
            if (client == except
                    && packet.getType() == Packet.PacketType.CREATE_ENTITY
                    && ((PacketCreateEntity) packet).entityId.equals(client.playerId)
            ) continue;

            client.sendTCP(packet);
        }
    }

    /**
     * Envoyer une position UDP à tous les clients sauf l'expéditeur
     */
    public static void broadcastPositionUDP(PacketEntityPosition packet, InetAddress exceptAddress, int exceptPort, UDPServer udpServer) {
        monitor.logUDPSent(packet);

        for (ClientConnection client : entitiesManager.getClients().values()) {
            int udpPort = client.getUdpPort();
            if (udpPort == 0) continue;

            if (client.getAddress().equals(exceptAddress) && udpPort == exceptPort) {
                continue;
            }
            udpServer.sendPosition(packet, client.getAddress(), udpPort);
        }
    }

    public static void broadcastBulkPositionUDP(EntitiesManager entitiesManager, UDPServer udpServer) {
        List<Entity> moved = entitiesManager.getEntities()
                .values()
                .stream()
                .filter(e -> e.dirtyPosition)
                .toList();

        if (moved.isEmpty()) return;

        //On divise la liste en petites listes de 17 entités pour éviter de passer au-dessus de la limite du buffer
        List<List<Entity>> dividedMoved = new ArrayList<>();
        List<Entity> currentList = new ArrayList<>();

        for (int i = 0; i < moved.size(); i++) {
            if (i % 17 == 0) {
                dividedMoved.add(new ArrayList<>());
                currentList = dividedMoved.get(dividedMoved.size() - 1);
            }

            currentList.add(moved.get(i));
        }

        //On sérialise toutes les listes
        List<byte[]> dividedDatas = new ArrayList<>();
        for (List<Entity> part : dividedMoved) {
            byte[] data = PacketSerializer.serializeBulk(new PacketPositionsBulk(part));
            dividedDatas.add(data);
        }

        for (ClientConnection client : entitiesManager.getClients().values()) {
            int udpPort = client.getUdpPort();
            if (udpPort == 0) continue;

            //Et on envoie tous les serials à chaque client
            for (byte[] data : dividedDatas) {
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(
                            data, data.length, client.getAddress(), udpPort
                    );
                    udpServer.socket.send(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
