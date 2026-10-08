package com.superkiment.server.entities;

import com.superkiment.common.Logger;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.net.EntitySync;
import com.superkiment.common.net.Packets.RemoveEntity;
import com.superkiment.common.net.Packets.UpdateEntity;
import com.superkiment.server.network.ClientConnection;
import com.superkiment.server.network.Network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source de vérité du monde. Toute entité ajoutée / modifiée / retirée ici est propagée automatiquement aux clients.
 */
public class ServerEntitiesManager extends EntitiesManager {
    private final Map<String, ClientConnection> clients = new ConcurrentHashMap<>();
    private final EntitySync.Tracker tracker = new EntitySync.Tracker();

    public Map<String, ClientConnection> getClients() {
        return clients;
    }

    @Override
    public void addEntity(Entity entity) {
        super.addEntity(entity);
        tracker.rebase(entity);
        Network.broadcast(EntitySync.spawnPacket(entity));
    }

    @Override
    public Entity removeEntity(String id) {
        Entity removed = super.removeEntity(id);
        if (removed != null) {
            tracker.forget(id);
            Network.broadcast(new RemoveEntity(id));
        }
        return removed;
    }

    /**
     * Envoie aux clients les champs @Sync modifiés depuis le dernier tick.
     */
    public void broadcastChanges() {
        for (Entity entity : entities.values()) {
            Map<String, Object> changes = tracker.diff(entity);
            if (changes != null) Network.broadcast(new UpdateEntity(entity.id, changes));
        }
    }

    public void removeClient(ClientConnection client) {
        if (client.playerId == null || !clients.remove(client.playerId, client)) return;

        Logger.info("Client déconnecté: " + client.playerName);
        removeEntity(client.playerId);
    }
}
