package com.superkiment.common.entities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La collection et gestion des entités et clients dans le monde.
 */
public class EntitiesManager {

    private final Map<String, Entity> entities = new ConcurrentHashMap<>();

    /**
     * Uniquement utilisé par le serveur.
     */

    public EntitiesManager() {}

    public Map<String, Entity> getEntities() {
        return entities;
    }





    public void addEntity(Entity entity) {
        entity.setEntitiesManager(this);
        entities.put(entity.id, entity);
    }
}
