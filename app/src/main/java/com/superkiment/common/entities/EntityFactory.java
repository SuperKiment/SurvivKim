package com.superkiment.common.entities;

import com.superkiment.common.packets.Packet;
import com.superkiment.common.packets.entity.PacketCreateEntity;
import com.superkiment.common.packets.entity.PacketCreateEntityProjectile;
import org.joml.Vector2d;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EntityFactory {
    private static EntityFactory instance;
    private final Map<Class<? extends Packet>, Function<Packet, Entity>> creators;

    private EntityFactory() {
        creators = new HashMap<>();

        creators.put(PacketCreateEntity.class, packet -> {
            PacketCreateEntity pe = (PacketCreateEntity) packet;

            Entity entity = new Entity(new Vector2d(pe.posX, pe.posY));
            entity.id = pe.entityId;
            entity.name = pe.entityName;

            System.out.println("USED ENTITY CREATOR");

            return entity;
        });

        creators.put(PacketCreateEntityProjectile.class, p -> {
            PacketCreateEntityProjectile pp = (PacketCreateEntityProjectile) p;
            Projectile projectile = new Projectile(new Vector2d(pp.posX, pp.posY), new Vector2d(pp.trajX, pp.trajY));

            projectile.id = pp.entityId;
            projectile.name = pp.entityName;

            System.out.println("USED PROJECTILE CREATOR");
            System.out.println(projectile.shapeModel.shapes.size());

            return projectile;
        });
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
}
