package com.superkiment.common.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La collection et gestion des entités et clients dans le monde.
 */
public class EntitiesManager {

    protected final Map<String, Entity> entities = new ConcurrentHashMap<>();
    protected final List<Entity> toBeDeletedEntities = new ArrayList<>();

    public EntitiesManager() {
    }

    public Map<String, Entity> getEntities() {
        return entities;
    }

    public void addEntity(Entity entity) {
        entity.setEntitiesManager(this);
        entities.put(entity.id, entity);
    }

    public void addToBeDeleted(Entity entity) {
        toBeDeletedEntities.add(entity);
    }

    public void deleteAllEntitiesToBeDeleted() {
        for (Entity entity : toBeDeletedEntities) {
            entity.onDeleted();
        }

        toBeDeletedEntities.clear();
    }

    public Entity getEntityFromID(String id) {
        return entities.get(id);
    }
}
