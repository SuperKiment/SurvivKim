package com.superkiment.common.entities;

import com.superkiment.server.ClientConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntitiesManager {

    private final Map<String, Entity> entities = new ConcurrentHashMap<>();
    private final Map<String, ClientConnection> clients = new ConcurrentHashMap<>();

    public EntitiesManager() {}

    public Map<String, Entity> getEntities() {
        return entities;
    }

    public Map<String, ClientConnection> getClients() {
        return clients;
    }
}
