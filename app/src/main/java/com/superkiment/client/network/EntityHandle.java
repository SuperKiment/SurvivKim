package com.superkiment.client.network;

import com.superkiment.client.Main;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.PacketCreateEntity;
import com.superkiment.common.packets.PacketDeleteEntity;
import org.joml.Vector2d;

import java.util.UUID;

import static com.superkiment.client.Main.entitiesManager;

public class EntityHandle {

    /**
     * Créer une entité (TCP - fiable)
     */
    public static void createEntity(String name, Vector2d position) {
        TCPClient tcpClient = Main.gameClient.getTCPClient();

        String entityId = UUID.randomUUID().toString();
        PacketCreateEntity packet = new PacketCreateEntity(entityId, name, position);
        tcpClient.send(packet);
    }

    /**
     * Supprimer une entité (TCP - fiable)
     */
    public static void deleteEntity(String entityId) {
        TCPClient tcpClient = Main.gameClient.getTCPClient();

        PacketDeleteEntity packet = new PacketDeleteEntity(entityId);
        tcpClient.send(packet);
    }


    public static void handleCreateEntity(PacketCreateEntity packet) {
        String playerId = Main.gameClient.getLocalPlayer().id;

        // Ne pas créer si c'est notre propre entité
        if (packet.entityId.equals(playerId)) {
            return;
        }

        Entity entity = new Entity(new Vector2d(packet.posX, packet.posY));
        entity.id = packet.entityId;
        entity.name = packet.entityName;

        entitiesManager.getEntities().put(entity.id, entity);
        System.out.println("Entité distante créée: " + entity.name + " (" + entity.id + ")");
    }

    public static void handleDeleteEntity(PacketDeleteEntity packet) {
        Entity removed = entitiesManager.getEntities().remove(packet.entityId);
        if (removed != null) {
            System.out.println("Entité supprimée: " + removed.name);
        }
    }
}
