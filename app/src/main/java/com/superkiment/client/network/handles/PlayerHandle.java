package com.superkiment.client.network.handles;

import com.superkiment.client.Main;
import com.superkiment.client.network.UDPClient;
import com.superkiment.common.entities.Player;
import com.superkiment.common.packets.entity.PacketEntityPosition;
import com.superkiment.common.packets.PacketPlayerJoin;

/**
 *  Le handle qui contient les fonctions nécessaires à la création de player et la récéption de données concernant la création de player.
 *  Contient également les fonctions sur la mise à jour de la position du joueur.
 */
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
