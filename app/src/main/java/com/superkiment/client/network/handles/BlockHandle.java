package com.superkiment.client.network.handles;

import com.superkiment.client.Main;
import com.superkiment.client.network.TCPClient;
import com.superkiment.common.packets.PacketCreateBlock;
import org.joml.Vector2d;

import static com.superkiment.client.Main.blocksManager;

/**
 * Le handle qui contient les fonctions nécessaires à la création de blocs et la récéption de données concernant la création de block.
 */
public class BlockHandle {

    /**
     * Créer un block (TCP - fiable)
     */
    public static void createBlock(Vector2d position) {
        TCPClient tcpClient = Main.gameClient.getTCPClient();
        PacketCreateBlock packet = new PacketCreateBlock(position);
        tcpClient.send(packet);
    }


    public static void handleCreateBlock(PacketCreateBlock packet) {
        blocksManager.addBlock(new Vector2d(packet.posX, packet.posY));
        System.out.println("Block distant créé: (" + packet.posX + " " + packet.posY + ")");
    }
}
