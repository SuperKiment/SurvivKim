package com.superkiment.server.network.handles;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.PacketCreateEntity;
import com.superkiment.common.packets.PacketDeleteEntity;
import com.superkiment.server.GameServer;
import com.superkiment.server.monitor.ServerMonitor;
import com.superkiment.server.network.ClientConnection;
import org.joml.Vector2d;

import static com.superkiment.server.network.Network.broadcastTCP;

public class EntityHandle {

    public static void handleCreateEntity(PacketCreateEntity packet, ClientConnection client) {
        Entity entity = new Entity(new Vector2d(packet.posX, packet.posY));
        entity.id = packet.entityId;
        entity.name = packet.entityName;

        GameServer.entitiesManager.getEntities().put(entity.id, entity);

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
}
