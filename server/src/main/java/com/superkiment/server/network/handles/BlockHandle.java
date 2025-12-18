package com.superkiment.server.network.handles;

import com.superkiment.common.packets.PacketCreateBlock;
import com.superkiment.server.GameServer;
import com.superkiment.server.monitor.ServerMonitor;
import com.superkiment.server.network.ClientConnection;
import org.joml.Vector2d;

import static com.superkiment.server.network.Network.broadcastTCP;

/**
 *  Le handle qui contient les fonctions nécessaires à la création de blocs et la récéption de données concernant la création de blocks.
 */
public class BlockHandle {

    public static void handleCreateBlock(PacketCreateBlock packet, ClientConnection client) {
        if (GameServer.blocksManager.addBlock(new Vector2d(packet.posX, packet.posY))) {
            broadcastTCP(packet, client);
            ServerMonitor.getInstance().log("INFO", "Block créé: " + packet.posX + " " + packet.posY);
        }
    }
}
