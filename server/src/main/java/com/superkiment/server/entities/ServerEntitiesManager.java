package com.superkiment.server.entities;

import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.entity.PacketDeleteEntity;
import com.superkiment.server.network.ClientConnection;
import com.superkiment.server.network.Network;
import com.superkiment.server.network.handles.EntityHandle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerEntitiesManager extends EntitiesManager {
    private final Map<String, ClientConnection> clients = new ConcurrentHashMap<>();

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

    @Override
    public void deleteAllEntitiesToBeDeleted() {
        for (Entity entity : toBeDeletedEntities) {
            entity.onDeleted();
            EntityHandle.deleteEntity(entity.id);
        }

        toBeDeletedEntities.clear();
    }
}
