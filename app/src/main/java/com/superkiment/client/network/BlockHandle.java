package com.superkiment.client.network;

import com.superkiment.client.Main;
import com.superkiment.common.packets.PacketCreateBlock;
import org.joml.Vector2d;

import java.util.UUID;

import static com.superkiment.client.Main.blocksManager;

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
