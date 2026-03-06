package com.superkiment.common.entities;

import com.superkiment.common.Logger;
import com.superkiment.common.packets.Packet;
import com.superkiment.common.packets.entity.PacketCreateEntity;
import com.superkiment.common.packets.entity.PacketCreateEntityPlayer;
import com.superkiment.common.packets.entity.PacketCreateEntityProjectile;
import org.joml.Vector2d;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Usine d'entités pour transformer des packets en entités prêtes à être ajoutées à l'EntitiesManager.
 */
public class EntityFactory {
    private static EntityFactory instance;
    public EntitiesManager entitiesManager;

    /**
     * Liaison entre une classe de Packet et une fonction qui crée une entité en fonction de cette même classe de Packet.
     */
    private final Map<Class<? extends Packet>, Function<Packet, Entity>> creators;

    private EntityFactory() {
        creators = new HashMap<>();

        creators.put(PacketCreateEntity.class, packet -> {
            Entity e = new Entity();
            return ApplyBasePacketToEntity((PacketCreateEntity) packet, e);
        });

        creators.put(PacketCreateEntityProjectile.class, p -> {
            PacketCreateEntityProjectile pp = (PacketCreateEntityProjectile) p;
            Projectile projectile = new Projectile(new Vector2d(pp.posX, pp.posY), new Vector2d(pp.trajX, pp.trajY));

            ApplyBasePacketToEntity(pp, projectile);

            // trouver les exceptions
            for (String id : pp.exceptions) {
                projectile.addCollisionException(entitiesManager.getEntityFromID(id));
            }

            Logger.log(Logger.LogLevel.DEBUG, "USED PROJECTILE CREATOR, " + projectile.numberOfCollisionExceptions() + " collision exceptions.");

            return projectile;
        });

        creators.put(PacketCreateEntityPlayer.class, p -> {
            PacketCreateEntityPlayer pp = (PacketCreateEntityPlayer) p;
            Player player = new Player(new Vector2d(pp.posX, pp.posY));

            ApplyBasePacketToEntity(pp, player);

            Logger.log(Logger.LogLevel.DEBUG, "USED PLAYER CREATOR");

            return player;
        });
    }

    /**
     * Utilisé une seule fois en début de client/serveur
     *
     * @param entitiesManager
     */
    public static void CreateInstance(EntitiesManager entitiesManager) {
        if (instance == null) {
            instance = new EntityFactory();
            instance.entitiesManager = entitiesManager;
            Logger.log(Logger.LogLevel.DEBUG, "EntitiesManager entity created");
        }
    }

    public static EntityFactory getInstance() {
        if (instance == null) instance = new EntityFactory();
        return instance;
    }

    public Entity create(Packet packet) {
        Function<Packet, Entity> creator = creators.get(packet.getClass());
        if (creator != null) {
            return creator.apply(packet);
        }
        throw new IllegalArgumentException("Unknown packet type: " + packet.getClass());
    }

    public static Entity ApplyBasePacketToEntity(PacketCreateEntity pe, Entity entity) {

        entity.id = pe.entityId;
        entity.name = pe.entityName;
        entity.pos = new Vector2d(pe.posX, pe.posY);
        entity.hp = pe.hp;

        if (entity.shapeModel.shapes.size() != pe.shapesTexts.length)
            throw new RuntimeException("While applying packet to entity : Entity shapes size != Packet shapes size !!");

        for (int i = 0; i < entity.shapeModel.shapes.size(); i++) {
            entity.shapeModel.shapes.get(i).text = pe.shapesTexts[i];
        }
        Logger.log(Logger.LogLevel.DEBUG, "USED ENTITY APPLY BASE");

        return entity;
    }
}
