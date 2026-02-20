package com.superkiment.client.network.handles;

import com.superkiment.client.Main;
import com.superkiment.client.network.TCPClient;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.EntityFactory;
import com.superkiment.common.packets.Packet;
import com.superkiment.common.packets.entity.LinkEntityPacket;
import com.superkiment.common.packets.entity.PacketCreateEntity;
import com.superkiment.common.packets.entity.PacketDeleteEntity;
import com.superkiment.common.packets.entity.PacketUpdateEntity;

import static com.superkiment.client.Main.entitiesManager;
import static com.superkiment.client.graphics.ui.dynamic_ui.ShapeModel_UI.AddDynamicUIElementsToEntity;

/**
 * Le handle qui contient les fonctions nécessaires à la création et suppression d'entités et la récéption de données concernant la création et suppression d'entités.
 */
public class EntityHandle {

    public static void createEntity(Entity entity) {
        Packet packet = LinkEntityPacket.CreatePacketFromEntity(entity);
        TCPClient tcpClient = Main.gameClient.getTCPClient();
        tcpClient.send(packet);
    }

    public static void updateEntity(Entity entity) {
        Packet packet = new PacketUpdateEntity(entity);
        TCPClient tcpClient = Main.gameClient.getTCPClient();
        tcpClient.send(packet);
    }

    public static void handleCreateEntity(PacketCreateEntity packet) {
        String playerId = Main.gameClient.getLocalPlayer().id;

        System.out.println("Handling create entity : " + packet.getClass().getName());

        // Ne pas créer si c'est notre propre entité
        if (packet.entityId.equals(playerId)) {
            return;
        }

        Entity entity = EntityFactory.getInstance().create(packet);

        // Ajouter les UI dynamiques
        AddDynamicUIElementsToEntity(entity);

        entitiesManager.addEntity(entity);
        System.out.println("Entité distante créée: " + entity.name + " (" + entity.id + ") à la position " + entity.pos);
    }

    public static void handleDeleteEntity(PacketDeleteEntity packet) {
        Entity removed = entitiesManager.getEntities().remove(packet.entityId);
        if (removed != null) {
            System.out.println("Entité supprimée: " + removed.name);
        }
    }

    public static void handleUpdateEntity(PacketCreateEntity packet) {

        System.out.println("Handling update entity : " + packet.getClass().getName());

        Entity entity = entitiesManager.getEntityFromID(packet.entityId);
        EntityFactory.ApplyBasePacketToEntity(packet, entity);

        System.out.println("Entité distante mise à jour: " + entity.name + " (" + entity.id + ") à la position " + entity.pos);
    }
}
