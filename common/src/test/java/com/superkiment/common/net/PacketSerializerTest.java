package com.superkiment.common.net;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Player;
import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de sérialisation / désérialisation TCP et UDP.
 * Ces tests couvrent le roundtrip complet : objet → bytes → objet.
 */
public class PacketSerializerTest {

    // ─────────────────────── TCP (Java Serialization) ────────────────────

    @Test
    void tcp_serialize_join_deserialize_isEqual() throws IOException, ClassNotFoundException {
        Packets.Join original = new Packets.Join("uuid-42", "Alice");

        byte[] bytes = PacketSerializer.serialize(original);
        assertTrue(bytes.length > 0, "Les bytes ne doivent pas être vides");

        assertEquals(original, PacketSerializer.deserialize(bytes), "Un record doit survivre au roundtrip");
    }

    @Test
    void tcp_serialize_spawnEntity_keepsState() throws IOException, ClassNotFoundException {
        Packets.SpawnEntity original = EntitySync.spawnPacket(new Player(new Vector2d(5, 6)));

        Packets.SpawnEntity result = (Packets.SpawnEntity) PacketSerializer.deserialize(PacketSerializer.serialize(original));

        assertEquals(original.id(), result.id());
        assertEquals(original.type(), result.type());
        assertEquals(original.state().get("respawnPoint"), result.state().get("respawnPoint"), "Vector2d doit être sérialisable");
    }

    // ─────────────────────── UDP (format compact) ────────────────────────

    @Test
    void udp_serializePosition_deserialize_roundtrip() {
        PacketEntityPosition original = new PacketEntityPosition(
                "entity-uuid-123",
                150.5, 300.25,
                0.707, 0.707
        );
        original.timestamp = 9999L;

        byte[] bytes = PacketSerializer.serializePositionUDP(original);

        // Le type est le premier byte (1 pour position)
        assertEquals(1, bytes[0], "Le premier byte doit être le type 1 (position)");

        // Désérialisation : on saute le premier byte de type
        ByteBuffer buffer = ByteBuffer.wrap(bytes, 1, bytes.length - 1);
        PacketEntityPosition result = PacketSerializer.deserializePositionUDP(buffer);

        assertEquals("entity-uuid-123", result.entityId, "entityId doit survivre");
        assertEquals(150.5,  result.posX,  1e-9, "posX doit survivre");
        assertEquals(300.25, result.posY,  1e-9, "posY doit survivre");
        assertEquals(0.707,  result.dirX,  1e-9, "dirX doit survivre");
        assertEquals(0.707,  result.dirY,  1e-9, "dirY doit survivre");
        assertEquals(9999L,  result.timestamp,    "timestamp doit survivre");
    }

    @Test
    void udp_serializePosition_longUuid_roundtrip() {
        String longId = "550e8400-e29b-41d4-a716-446655440000";
        PacketEntityPosition original = new PacketEntityPosition(longId, 0, 0, 0, 1);

        byte[] bytes = PacketSerializer.serializePositionUDP(original);
        ByteBuffer buffer = ByteBuffer.wrap(bytes, 1, bytes.length - 1);
        PacketEntityPosition result = PacketSerializer.deserializePositionUDP(buffer);

        assertEquals(longId, result.entityId, "UUID complet doit survivre");
    }

    // ─────────────────────── UDP Bulk ────────────────────────────────────

    @Test
    void udp_serializeBulk_deserialize_roundtrip() {
        // Construire un bulk avec 3 entités
        Entity e1 = new Entity(new Vector2d(10, 20));
        Entity e2 = new Entity(new Vector2d(30, 40));
        Entity e3 = new Entity(new Vector2d(50, 60));

        List<Entity> entities = List.of(e1, e2, e3);
        PacketPositionsBulk original = new PacketPositionsBulk(entities);

        byte[] bytes = PacketSerializer.serializeBulk(original);
        assertEquals(2, bytes[0], "Bulk doit avoir le type 2");

        ByteBuffer buffer = ByteBuffer.wrap(bytes, 1, bytes.length - 1);
        PacketPositionsBulk result = PacketSerializer.deserializeBulkPositions(buffer);

        assertEquals(3, result.ids.size(), "3 entités dans le bulk");
        assertTrue(result.ids.contains(e1.id), "ID de e1 doit être présent");
        assertTrue(result.ids.contains(e2.id), "ID de e2 doit être présent");
        assertTrue(result.ids.contains(e3.id), "ID de e3 doit être présent");

        int idx1 = result.ids.indexOf(e1.id);
        assertEquals(10, result.x.get(idx1), 1e-9, "posX de e1");
        assertEquals(20, result.y.get(idx1), 1e-9, "posY de e1");
    }

    @Test
    void udp_serializeBulk_empty_roundtrip() {
        PacketPositionsBulk empty = new PacketPositionsBulk(new ArrayList<>());
        byte[] bytes = PacketSerializer.serializeBulk(empty);

        ByteBuffer buffer = ByteBuffer.wrap(bytes, 1, bytes.length - 1);
        PacketPositionsBulk result = PacketSerializer.deserializeBulkPositions(buffer);

        assertEquals(0, result.ids.size(), "Un bulk vide doit rester vide");
    }
}
