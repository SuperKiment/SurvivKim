package com.superkiment.server.network.handles;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.EntityFactory;
import com.superkiment.common.packets.entity.PacketCreateEntity;
import com.superkiment.common.packets.entity.PacketDeleteEntity;
import com.superkiment.common.packets.entity.PacketUpdateEntity;
import com.superkiment.server.GameServer;
import com.superkiment.server.monitor.ServerMonitor;
import com.superkiment.server.network.ClientConnection;

import static com.superkiment.server.network.Network.broadcastTCP;

/**
 * Le handle qui contient les fonctions nécessaires à la création et suppression d'entités et la récéption de données concernant la création et suppression d'entités.
 */
public class EntityHandle {

    public static void deleteEntity(String id) {
        PacketDeleteEntity packet = new PacketDeleteEntity(id);
        handleDeleteEntity(packet);
    }

    public static void handleCreateEntity(PacketCreateEntity packet, ClientConnection client) {
        Entity entity = EntityFactory.getInstance().create(packet);

        GameServer.entitiesManager.addEntity(entity);

        // Broadcaster à tous les clients
        broadcastTCP(packet, client);
        ServerMonitor.getInstance().log("INFO", "Entité créée: " + entity.id + " (" + entity.name + ")");
    }

    public static void handleDeleteEntity(PacketDeleteEntity packet) {
        GameServer.entitiesManager.getEntities().remove(packet.entityId);
        System.out.println("Entité supprimée: " + packet.entityId);

        // Broadcaster à tous les clients
        broadcastTCP(packet, null);
        ServerMonitor.getInstance().log("INFO", "Entité supprimée: " + packet.entityId);
    }

    public static void handleUpdateEntity(PacketUpdateEntity packet, ClientConnection client) {
        Entity entity = GameServer.entitiesManager.getEntityFromID(packet.entityId);
        EntityFactory.ApplyBasePacketToEntity(packet, entity);

        broadcastTCP(packet, client);
        ServerMonitor.getInstance().log("INFO", "Entité mise à jour: " + entity.id + " (" + entity.name + ")");
    }
}
