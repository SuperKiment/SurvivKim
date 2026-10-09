package com.superkiment.common.net;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Player;
import com.superkiment.common.entities.Projectile;
import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie qu'une entité traverse le réseau sans aucun code dédié : seulement des champs @Sync.
 */
public class EntitySyncTest {

    /**
     * Exemple du workflow attendu : une nouvelle entité = une classe, rien d'autre.
     */
    public static class Zombie extends Entity {
        @Sync
        public int rage = 1;
        @Sync
        public Vector2d target = new Vector2d();
        public int notSynced = 0;
    }

    private static Entity roundtrip(Entity e) {
        return EntitySync.create(EntitySync.spawnPacket(e));
    }

    // ───────────────────── Spawn ─────────────────────────────────────────

    @Test
    void spawn_customEntity_withoutAnyNetworkCode() {
        Zombie z = new Zombie();
        z.rage = 7;
        z.target.set(3, 4);
        z.notSynced = 99;

        Zombie copy = (Zombie) roundtrip(z);

        assertEquals(z.id, copy.id);
        assertEquals(7, copy.rage);
        assertEquals(new Vector2d(3, 4), copy.target);
        assertNotSame(z.target, copy.target, "Les Vector2d doivent être copiés, pas partagés");
        assertEquals(0, copy.notSynced, "Un champ non annoté ne doit pas voyager");
    }

    @Test
    void spawn_player_keepsTransformAndInheritedFields() {
        Player p = new Player(new Vector2d(75, 125));
        p.name = "Alpha";
        p.hp = 80;

        Entity copy = roundtrip(p);

        assertInstanceOf(Player.class, copy);
        assertEquals(new Vector2d(75, 125), copy.pos);
        assertEquals(copy.pos, copy.posLerp, "Pas de glissement visuel au spawn");
        assertEquals("Alpha", copy.name);
        assertEquals(80f, copy.hp, 1e-4f, "Champ @Sync hérité de Collisionable");
    }

    @Test
    void spawn_projectile_keepsTrajectoryAndOwner() {
        Player owner = new Player(new Vector2d());
        Projectile proj = new Projectile(new Vector2d(200, 300), new Vector2d(0, 1));
        proj.ownerId = owner.id;

        Projectile copy = (Projectile) roundtrip(proj);

        assertEquals(new Vector2d(200, 300), copy.pos);
        assertEquals(new Vector2d(0, 1), copy.trajectory);
        assertTrue(copy.hasCollisionException(owner), "Le tireur ne doit pas être touché");
        assertFalse(copy.hasCollisionException(new Player(new Vector2d())));
    }

    @Test
    void spawn_keepsShapeTexts() {
        Player p = new Player(new Vector2d());
        p.shapeModel.shapes.get(0).text = "hello";

        assertEquals("hello", roundtrip(p).shapeModel.shapes.get(0).text);
    }

    @Test
    void create_unknownType_throws() {
        var packet = new Packets.SpawnEntity("com.nope.Ghost", "id", 0, 0, 0, 1, Map.of());
        assertThrows(IllegalArgumentException.class, () -> EntitySync.create(packet));
    }

    // ───────────────────── Tracker ───────────────────────────────────────

    @Test
    void tracker_firstDiff_isBaseline() {
        assertNull(new EntitySync.Tracker().diff(new Zombie()));
    }

    @Test
    void tracker_diff_returnsOnlyChangedFields() {
        EntitySync.Tracker tracker = new EntitySync.Tracker();
        Zombie z = new Zombie();
        tracker.diff(z);

        z.rage = 3;
        z.target.x = 10; // mutation en place détectée grâce à la copie du snapshot

        Map<String, Object> changes = tracker.diff(z);
        assertEquals(Map.of("rage", 3, "target", new Vector2d(10, 0)), changes);
        assertNull(tracker.diff(z), "Plus rien à envoyer une fois le diff consommé");
    }

    @Test
    void tracker_rebase_preventsEcho() {
        EntitySync.Tracker tracker = new EntitySync.Tracker();
        Zombie z = new Zombie();
        tracker.diff(z);

        EntitySync.apply(z, Map.of("rage", 42));
        tracker.rebase(z);

        assertNull(tracker.diff(z));
    }

    @Test
    void apply_update_ignoresMissingKeys() {
        Zombie z = new Zombie();
        z.rage = 5;
        EntitySync.apply(z, Map.of("name", "Bob"));

        assertEquals("Bob", z.name);
        assertEquals(5, z.rage);
    }
}
