package com.superkiment.common.packets.entity;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.Packet;
import com.superkiment.common.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Permet de trouver automatiquement le paquet lié à l'entité et permettre la création de cette dernière dans le EntityCreator.
 */
public class LinkEntityPacket {
    public static Map<Class<? extends Entity>, IPacketCreator> entityToPacketCreator = new HashMap<>();

    private static void LinkEntityToPacket(Entity entity) {
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
        return entity1 -> {
            try {
                return (Packet) instanciateMethod.invoke(null, entity1);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate packet via instanciateFromEntity", e);
            }
        };
    }

    public static Packet CreatePacketFromEntity(Entity entity) {
        // Si l'entrée n'existe pas, l'ajouter d'abord
        if (!entityToPacketCreator.containsKey(entity.getClass())) LinkEntityToPacket(entity);

        return entityToPacketCreator.get(entity.getClass()).instantiateFromEntity(entity);
    }
}
