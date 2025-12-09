package com.superkiment.server.network.handles;

import com.superkiment.common.blocks.Block;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.PacketCreateBlock;
import com.superkiment.common.packets.PacketCreateEntity;
import com.superkiment.common.packets.PacketPlayerJoin;
import com.superkiment.server.GameServer;
import com.superkiment.server.monitor.ServerMonitor;
import com.superkiment.server.network.ClientConnection;

import static com.superkiment.server.network.Network.broadcastTCP;

public class PlayerHandle {

    public static void handlePlayerJoin(PacketPlayerJoin packet, ClientConnection client) {
        client.playerId = packet.playerId;
        client.playerName = packet.playerName;
        GameServer.entitiesManager.getClients().put(packet.playerId, client);

        System.out.println("Joueur connecté: " + packet.playerName + " (" + packet.playerId + ")");

        // Envoyer toutes les entités existantes au nouveau joueur
        for (Entity entity : GameServer.entitiesManager.getEntities().values()) {
            PacketCreateEntity createPacket = new PacketCreateEntity(
                    entity.id, entity.name, entity.pos
            );
            client.sendTCP(createPacket);
        }

        for (Block block : GameServer.blocksManager.getBlocks()) {
            PacketCreateBlock packetCreateBlock = new PacketCreateBlock(block.pos);
            client.sendTCP(packetCreateBlock);
        }

        // Broadcaster le nouveau joueur aux autres
        broadcastTCP(packet, client);
        ServerMonitor.getInstance().log("INFO", "Joueur a rejoint : " + client.playerId + " (" + client.playerName + ")");
    }
}
