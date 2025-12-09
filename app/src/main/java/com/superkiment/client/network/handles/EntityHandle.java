package com.superkiment.client.network.handles;

import com.superkiment.client.Main;
import com.superkiment.client.network.TCPClient;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.EntityFactory;
import com.superkiment.common.entities.Projectile;
import com.superkiment.common.packets.Packet;
import com.superkiment.common.packets.entity.IPacketCreator;
import com.superkiment.common.packets.entity.PacketCreateEntity;
import com.superkiment.common.packets.entity.PacketCreateEntityProjectile;
import com.superkiment.common.packets.entity.PacketDeleteEntity;
import com.superkiment.common.utils.StringUtils;
import org.joml.Vector2d;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.superkiment.client.Main.entitiesManager;

public class EntityHandle {

    private static Map<Class<? extends Entity>, IPacketCreator> entityToPacketCreator = new HashMap<>();

    static {
    }

    public static void createEntity(Entity entity) {
        // Si l'entrée n'existe pas, l'ajouter d'abord
        if (!entityToPacketCreator.containsKey(entity.getClass())) linkEntityToPacket(entity);

        Packet packet = entityToPacketCreator.get(entity.getClass()).instanciateFromEntity(entity);

        TCPClient tcpClient = Main.gameClient.getTCPClient();
        tcpClient.send(packet);
    }

    public static void linkEntityToPacket(Entity entity) {
        Class<? extends Entity> entityClass = entity.getClass();

        // Vérifie si un créateur est déjà enregistré
        if (entityToPacketCreator.containsKey(entityClass)) return;

        String entitySimpleName = StringUtils.GetLastTerm(entityClass.getName());
        String packetPackage = "com.superkiment.common.packets.entity";
        String packetClassName;

        // Cas particulier pour Entity de base
        if (entitySimpleName.equals("Entity")) {
            packetClassName = packetPackage + ".PacketCreateEntity";
        } else {
            packetClassName = packetPackage + ".PacketCreateEntity" + entitySimpleName;
        }

        try {
            // Charge dynamiquement la classe du packet
            Class<?> packetClass = Class.forName(packetClassName);

            // Vérifie que la classe a bien une méthode statique instanciateFromEntity
            IPacketCreator creator = getIPacketCreator(packetClass);

            // Enregistre dans la map
            entityToPacketCreator.put(entityClass, creator);

        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private static IPacketCreator getIPacketCreator(Class<?> packetClass) throws NoSuchMethodException {
        Method instanciateMethod = packetClass.getDeclaredMethod("instanciateFromEntity", Entity.class);

        // Crée un PacketCreator qui appelle la méthode statique
        IPacketCreator creator = entity1 -> {
            try {
                return (Packet) instanciateMethod.invoke(null, entity1);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate packet via instanciateFromEntity", e);
            }
        };
        return creator;
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

        System.out.println("Handling create entity : " + packet.getClass().getName());

        // Ne pas créer si c'est notre propre entité
        if (packet.entityId.equals(playerId)) {
            return;
        }

        Entity entity = EntityFactory.getInstance().create(packet);

        entitiesManager.getEntities().put(entity.id, entity);
        System.out.println("Entité distante créée: " + entity.name + " (" + entity.id + ") à la position " + entity.pos);
    }

    public static void handleDeleteEntity(PacketDeleteEntity packet) {
        Entity removed = entitiesManager.getEntities().remove(packet.entityId);
        if (removed != null) {
            System.out.println("Entité supprimée: " + removed.name);
        }
    }
}
