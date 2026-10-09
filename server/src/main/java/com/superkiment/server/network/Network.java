package com.superkiment.server.network;

import com.superkiment.common.Logger;
import com.superkiment.common.blocks.Block;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.net.*;
import com.superkiment.common.net.Packets.*;
import com.superkiment.server.GameServer;
import com.superkiment.server.entities.ServerEntitiesManager;
import com.superkiment.server.monitor.ServerMonitor;
import org.joml.Vector2d;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Réception (routes TCP) et envoi de paquets côté serveur.
 */
public class Network {

    private static final ServerMonitor monitor = ServerMonitor.getInstance();

    /**
     * Toutes les routes TCP du serveur. Exécutées sur le thread de jeu via {@link PacketRouter#drain()}.
     */
    public static final PacketRouter<ClientConnection> ROUTER = new PacketRouter<ClientConnection>()
            .on(Join.class, Network::onJoin)
            .on(Heartbeat.class, (p, client) -> client.onHeartbeat(p.timestamp()))
            .on(SpawnEntity.class, (p, client) -> entities().addEntity(EntitySync.create(p)))
            .on(UpdateEntity.class, (p, client) -> {
                Entity entity = entities().getEntityFromID(p.id());
                if (entity != null) EntitySync.apply(entity, p.changes());
            })
            .on(RemoveEntity.class, (p, client) -> entities().removeEntity(p.id()))
            .on(CreateBlock.class, (p, client) -> {
                if (GameServer.blocksManager.addBlock(new Vector2d(p.x(), p.y()))) broadcast(p);
            });

    private static ServerEntitiesManager entities() {
        return GameServer.entitiesManager;
    }

    private static void onJoin(Join packet, ClientConnection client) {
        client.playerId = packet.playerId();
        client.playerName = packet.playerName();
        entities().getClients().put(client.playerId, client);

        // Rattrapage de l'état du monde pour le nouveau venu
        for (Entity entity : entities().getEntities().values()) {
            client.send(EntitySync.spawnPacket(entity));
        }
        for (Block block : GameServer.blocksManager.getBlocks()) {
            client.send(new CreateBlock((int) block.pos.x, (int) block.pos.y));
        }

        broadcast(packet);
        Logger.info("Joueur connecté: " + client.playerName + " (" + client.playerId + ")");
        monitor.log("INFO", "Joueur a rejoint : " + client.playerId + " (" + client.playerName + ")");
    }

    /**
     * Envoyer un packet TCP à tous les clients connectés.
     */
    public static void broadcast(Packet packet) {
        monitor.logTCPSent(packet);
        for (ClientConnection client : entities().getClients().values()) {
            client.send(packet);
        }
    }

    /**
     * Gérer les packets UDP sur la mise à jour d'une seule entité reçus.
     */
    public static void handleUDPPacket(PacketEntityPosition packet, InetAddress address, int port, UDPServer udpServer) {
        monitor.logUDPReceived(packet);

        ClientConnection client = entities().getClients().get(packet.entityId);
        if (client != null && client.getUdpPort() == 0) {
            client.setUdpPort(port);
        }

        Entity entity = entities().getEntities().get(packet.entityId);
        if (entity != null) {
            entity.pos.set(packet.posX, packet.posY);
            entity.dirLookTarget.set(packet.dirX, packet.dirY);

            broadcastPositionUDP(packet, address, port, udpServer);
        }
    }

    /**
     * Envoyer une position UDP à tous les clients sauf l'expéditeur (mise à jour d'une seule entité).
     */
    public static void broadcastPositionUDP(PacketEntityPosition packet, InetAddress exceptAddress, int exceptPort, UDPServer udpServer) {
        monitor.logUDPSent(packet);

        for (ClientConnection client : entities().getClients().values()) {
            int udpPort = client.getUdpPort();
            if (udpPort == 0) continue;

            if (client.getAddress().equals(exceptAddress) && udpPort == exceptPort) {
                continue;
            }
            udpServer.sendPosition(packet, client.getAddress(), udpPort);
        }
    }

    /**
     * Envoyer en UDP les positions de toutes les entités qui ont bougé ce tick.
     */
    public static void broadcastBulkPositionUDP(UDPServer udpServer) {
        List<Entity> moved = entities().getEntities()
                .values()
                .stream()
                .filter(e -> e.dirtyPosition)
                .toList();

        if (moved.isEmpty()) return;

        // Paquets de 17 entités pour rester sous la taille du buffer de réception (1024 octets)
        List<byte[]> datas = new ArrayList<>();
        for (int i = 0; i < moved.size(); i += 17) {
            List<Entity> part = moved.subList(i, Math.min(i + 17, moved.size()));
            datas.add(PacketSerializer.serializeBulk(new PacketPositionsBulk(part)));
        }

        for (ClientConnection client : entities().getClients().values()) {
            int udpPort = client.getUdpPort();
            if (udpPort == 0) continue;

            for (byte[] data : datas) {
                try {
                    udpServer.socket.send(new DatagramPacket(data, data.length, client.getAddress(), udpPort));
                } catch (IOException e) {
                    Logger.error("Échec d'envoi UDP", e);
                }
            }
        }
    }
}
