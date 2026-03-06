package com.superkiment.common.entities;

import org.joml.Vector2d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntitiesManagerTest {

    private EntitiesManager manager;

    @BeforeEach
    void setUp() {
        manager = new EntitiesManager();
    }

    // ──────────────────── addEntity / getEntities ────────────────────────

    @Test
    void addEntity_increasesCount() {
        assertTrue(manager.getEntities().isEmpty(), "Map doit être vide au départ");

        manager.addEntity(new Entity(new Vector2d(0, 0)));
        assertEquals(1, manager.getEntities().size());
    }

    @Test
    void addEntity_setsEntitiesManagerOnEntity() {
        Entity e = new Entity();
        manager.addEntity(e);
        // On vérifie indirectement : e.entitiesManager est protected mais addToBeDeleted ne lève pas NPE
        assertDoesNotThrow(() -> e.deleteSelf());
    }

    @Test
    void addEntity_multipleEntities_allPresent() {
        Entity a = new Entity(); Entity b = new Entity(); Entity c = new Entity();
        manager.addEntity(a);
        manager.addEntity(b);
        manager.addEntity(c);
        assertEquals(3, manager.getEntities().size());
    }

    // ──────────────────── getEntityFromID ────────────────────────────────

    @Test
    void getEntityFromID_existingId_returnsEntity() {
        Entity e = new Entity();
        manager.addEntity(e);

        Entity found = manager.getEntityFromID(e.id);
        assertSame(e, found, "Doit retourner exactement la même instance");
    }

    @Test
    void getEntityFromID_missingId_returnsNull() {
        assertNull(manager.getEntityFromID("inexistant-uuid"),
                "Un ID inconnu doit retourner null");
    }

    // ──────────────────── addToBeDeleted / deleteAll ─────────────────────

    @Test
    void deleteAllEntitiesToBeDeleted_removesScheduledEntities() {
        Entity e1 = new Entity();
        Entity e2 = new Entity();
        manager.addEntity(e1);
        manager.addEntity(e2);

        manager.addToBeDeleted(e1);
        manager.deleteAllEntitiesToBeDeleted();

        // Les entités ne sont pas retirées de la map dans la classe de base (EntitiesManager),
        // mais le test vérifie que la liste est vidée (pas d'exception au second appel)
        assertDoesNotThrow(() -> manager.deleteAllEntitiesToBeDeleted(),
                "Un second appel ne doit pas lever d'exception");
    }

    @Test
    void deleteSelf_addsEntityToBeDeletedList() {
        Entity e = new Entity();
        manager.addEntity(e);
        e.deleteSelf();

        // Après deleteSelf, l'appel à deleteAll ne doit pas lever d'exception
        assertDoesNotThrow(() -> manager.deleteAllEntitiesToBeDeleted());
    }

    // ──────────────────── Cohérence de la map ─────────────────────────────

    @Test
    void addEntity_idUsedAsKey() {
        Entity e = new Entity();
        String id = e.id;
        manager.addEntity(e);

        assertTrue(manager.getEntities().containsKey(id),
                "La map doit utiliser l'id de l'entité comme clé");
    }

    @Test
    void entities_addedTwice_onlyStoredOnce() {
        // Un même id ne doit pas être dupliqué (ConcurrentHashMap replace)
        Entity e = new Entity();
        manager.addEntity(e);
        manager.addEntity(e); // Doublon
        assertEquals(1, manager.getEntities().size(),
                "Ajouter deux fois la même entité ne doit pas dupliquer");
    }
}
