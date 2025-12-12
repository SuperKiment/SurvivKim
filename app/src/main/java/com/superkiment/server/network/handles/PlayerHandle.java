package com.superkiment.server.network.handles;

import com.superkiment.common.blocks.Block;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.Packet;
import com.superkiment.common.packets.PacketCreateBlock;
import com.superkiment.common.packets.entity.LinkEntityPacket;
import com.superkiment.common.packets.PacketPlayerJoin;
import com.superkiment.server.GameServer;
import com.superkiment.server.monitor.ServerMonitor;
import com.superkiment.server.network.ClientConnection;

import static com.superkiment.server.network.Network.broadcastTCP;

/**
 * Le handle qui contient les fonctions nécessaires à la création de joueurs et la récéption de données concernant la création de joueurs.
 */
public class PlayerHandle {

    public static void handlePlayerJoin(PacketPlayerJoin packetPlayerJoin, ClientConnection client) {
        client.playerId = packetPlayerJoin.playerId;
        client.playerName = packetPlayerJoin.playerName;
        GameServer.entitiesManager.getClients().put(packetPlayerJoin.playerId, client);

        System.out.println("Joueur connecté: " + packetPlayerJoin.playerName + " (" + packetPlayerJoin.playerId + ")");

        // Envoyer toutes les entités existantes au nouveau joueur
        for (Entity entity : GameServer.entitiesManager.getEntities().values()) {
            Packet packet = LinkEntityPacket.CreatePacketFromEntity(entity);
            client.sendTCP(packet);
        }

        for (Block block : GameServer.blocksManager.getBlocks()) {
            PacketCreateBlock packetCreateBlock = new PacketCreateBlock(block.pos);
            client.sendTCP(packetCreateBlock);
        }

        // Broadcaster le nouveau joueur aux autres
        broadcastTCP(packetPlayerJoin, client);
        ServerMonitor.getInstance().log("INFO", "Joueur a rejoint : " + client.playerId + " (" + client.playerName + ")");
    }
}
