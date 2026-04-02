package com.superkiment.client.network.handles;

import com.superkiment.client.Main;
import com.superkiment.client.network.TCPClient;
import com.superkiment.common.Logger;
import com.superkiment.common.blocks.Block;
import com.superkiment.common.packets.PacketCreateBlock;

import static com.superkiment.client.Main.blocksManager;

/**
 * Le handle qui contient les fonctions nécessaires à la création de blocs et la récéption de données concernant la création de block.
 */
public class BlockHandle {

    /**
     * Créer un block (TCP - fiable)
     */
    public static void createBlock(Block block) {
        TCPClient tcpClient = Main.gameClient.getTCPClient();
        PacketCreateBlock packet = new PacketCreateBlock(block);
        tcpClient.send(packet);
    }


    public static void handleCreateBlock(PacketCreateBlock packet) {
        blocksManager.addBlock(new Block(packet.posX, packet.posY, packet.blockCollisionType));
        Logger.info("Block distant créé: (" + packet.posX + " " + packet.posY + ")");
    }
}
