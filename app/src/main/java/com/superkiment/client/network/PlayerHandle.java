package com.superkiment.client.network;

import com.superkiment.client.Main;
import com.superkiment.common.entities.Player;
import com.superkiment.common.packets.PacketEntityPosition;
import com.superkiment.common.packets.PacketPlayerJoin;

public class PlayerHandle {


    public static void handlePlayerJoin(PacketPlayerJoin packet) {
        String playerId = Main.gameClient.getLocalPlayer().id;

        // Ne rien faire si c'est nous
        if (packet.playerId.equals(playerId)) {
            return;
        }

        System.out.println("Joueur rejoint: " + packet.playerName);
    }

    /**
     * Envoyer la position du joueur (UDP)
     */
    public static void sendPosition() {
        UDPClient udpClient = Main.gameClient.getUDPClient();
        boolean connected = Main.gameClient.isConnected();
        Player localPlayer = Main.gameClient.getLocalPlayer();

        if (!connected || localPlayer == null) return;

        PacketEntityPosition packet = new PacketEntityPosition(
                localPlayer.id,
                localPlayer.pos.x,
                localPlayer.pos.y,
                localPlayer.dirLookTarget.x,
                localPlayer.dirLookTarget.y
        );

        udpClient.sendPosition(packet);
    }
}
