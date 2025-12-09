package com.superkiment.common.entities;

import com.superkiment.common.packets.entity.PacketDeleteEntity;
import com.superkiment.server.network.ClientConnection;
import com.superkiment.server.network.Network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntitiesManager {

    private final Map<String, Entity> entities = new ConcurrentHashMap<>();
    private final Map<String, ClientConnection> clients = new ConcurrentHashMap<>();

    public EntitiesManager() {}

    public Map<String, Entity> getEntities() {
        return entities;
    }

    public Map<String, ClientConnection> getClients() {
        return clients;
    }


    public void removeClient(ClientConnection client) {
        if (client.playerId != null) {
            getClients().remove(client.playerId);
            System.out.println("Client déconnecté: " + client.playerName);

            // Supprimer l'entité du joueur
            if (getEntities().containsKey(client.playerId)) {
                PacketDeleteEntity packet = new PacketDeleteEntity(client.playerId);
                Network.broadcastTCP(packet, null);
                getEntities().remove(client.playerId);
            }
        }
    }
}
