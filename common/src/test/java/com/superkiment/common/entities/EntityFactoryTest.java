package com.superkiment.common.entities;

import com.superkiment.common.packets.entity.PacketCreateEntityPlayer;
import com.superkiment.common.packets.entity.PacketCreateEntityProjectile;
import org.joml.Vector2d;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour EntityFactory.
 * Vérifie que la factory reconstruit des entités fidèles à partir des packets.
 */
public class EntityFactoryTest {

    private EntitiesManager entitiesManager;

    @BeforeEach
    void setUp() throws Exception {
        // Réinitialiser le singleton entre chaque test
        Field f = EntityFactory.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);

        entitiesManager = new EntitiesManager();
        EntityFactory.CreateInstance(entitiesManager);
    }

    // ───────────────────── Player ─────────────────────────────────────────

    @Test
    void create_fromPlayerPacket_returnsPlayerInstance() {
        Player originalPlayer = new Player(new Vector2d(100, 200));
        originalPlayer.name = "TestPlayer";

        PacketCreateEntityPlayer packet = new PacketCreateEntityPlayer(originalPlayer);
        Entity created = EntityFactory.getInstance().create(packet);

        assertInstanceOf(Player.class, created, "La factory doit créer un Player pour PacketCreateEntityPlayer");
    }

    @Test
    void create_fromPlayerPacket_positionMatches() {
        Player originalPlayer = new Player(new Vector2d(75, 125));
        PacketCreateEntityPlayer packet = new PacketCreateEntityPlayer(originalPlayer);

        Entity created = EntityFactory.getInstance().create(packet);

        assertEquals(75,  created.pos.x, 1e-9, "posX doit correspondre");
        assertEquals(125, created.pos.y, 1e-9, "posY doit correspondre");
    }

    @Test
    void create_fromPlayerPacket_nameAndIdMatch() {
        Player originalPlayer = new Player(new Vector2d(0, 0));
        originalPlayer.name = "Joueur Alpha";

        PacketCreateEntityPlayer packet = new PacketCreateEntityPlayer(originalPlayer);
        Entity created = EntityFactory.getInstance().create(packet);

        assertEquals(originalPlayer.id,   created.id,   "L'id doit être préservé");
        assertEquals("Joueur Alpha",       created.name, "Le nom doit être préservé");
    }

    @Test
    void create_fromPlayerPacket_hpMatches() {
        Player originalPlayer = new Player(new Vector2d(0, 0));
        originalPlayer.hp = 80f;

        PacketCreateEntityPlayer packet = new PacketCreateEntityPlayer(originalPlayer);
        Entity created = EntityFactory.getInstance().create(packet);

        assertEquals(80f, created.hp, 1e-4f, "Les HP doivent être préservés");
    }

    // ───────────────────── Projectile ─────────────────────────────────────

    @Test
    void create_fromProjectilePacket_returnsProjectileInstance() {
        Projectile projectile = new Projectile(new Vector2d(50, 50), new Vector2d(1, 0));

        PacketCreateEntityProjectile packet = new PacketCreateEntityProjectile(projectile);
        Entity created = EntityFactory.getInstance().create(packet);

        assertInstanceOf(Projectile.class, created, "La factory doit créer un Projectile");
    }

    @Test
    void create_fromProjectilePacket_positionMatches() {
        Projectile projectile = new Projectile(new Vector2d(200, 300), new Vector2d(0, 1));
        PacketCreateEntityProjectile packet = new PacketCreateEntityProjectile(projectile);

        Entity created = EntityFactory.getInstance().create(packet);

        assertEquals(200, created.pos.x, 1e-9);
        assertEquals(300, created.pos.y, 1e-9);
    }

    // ───────────────────── Erreur ────────────────────────────────────────

    @Test
    void create_unknownPacket_throwsIllegalArgument() {
        // Un packet anonyme non enregistré dans la factory
        var unknownPacket = new com.superkiment.common.packets.PacketPlayerJoin("id", "name");

        assertThrows(IllegalArgumentException.class,
                () -> EntityFactory.getInstance().create(unknownPacket),
                "Un packet non supporté doit lever une IllegalArgumentException");
    }

    // ───────────────────── Singleton ─────────────────────────────────────

    @Test
    void getInstance_afterCreateInstance_returnsSameInstance() {
        EntityFactory f1 = EntityFactory.getInstance();
        EntityFactory f2 = EntityFactory.getInstance();
        assertSame(f1, f2, "getInstance doit toujours retourner la même instance");
    }

    @Test
    void createInstance_calledTwice_doesNotReplaceFirst() throws Exception {
        // Le premier CreateInstance a déjà été appelé dans @BeforeEach
        EntitiesManager otherManager = new EntitiesManager();
        EntityFactory.CreateInstance(otherManager); // Doit être ignoré

        // L'instance doit avoir le premier entitiesManager
        assertSame(entitiesManager, EntityFactory.getInstance().entitiesManager,
                "CreateInstance ne doit pas remplacer l'instance existante");
    }
}
