package com.superkiment.common.packets;

import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.entity.PacketEntityPosition;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

public class PacketSerializer {

    /**
     * Sérialiser un packet en byte array (pour TCP)
     */
    public static byte[] serialize(Packet packet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(packet);
        oos.flush();
        return baos.toByteArray();
    }

    /**
     * Désérialiser un byte array en packet (pour TCP)
     */
    public static Packet deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (Packet) ois.readObject();
    }

    /**
     * Sérialiser un PacketEntityPosition en byte array compact (pour UDP)
     * Format: [entityId length][entityId][posX][posY][dirX][dirY][timestamp]
     */
    public static byte[] serializePositionUDP(PacketEntityPosition packet) {
        byte[] idBytes = packet.entityId.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + idBytes.length + 8 * 4 + 8);

        buffer.put((byte) 1);
        buffer.putInt(idBytes.length);
        buffer.put(idBytes);
        buffer.putDouble(packet.posX);
        buffer.putDouble(packet.posY);
        buffer.putDouble(packet.dirX);
        buffer.putDouble(packet.dirY);
        buffer.putLong(packet.timestamp);

        return buffer.array();
    }

    /**
     * Désérialiser un byte array en PacketEntityPosition (pour UDP)
     */
    public static PacketEntityPosition deserializePositionUDP(ByteBuffer buffer) {

        int idLength = buffer.getInt();
        byte[] idBytes = new byte[idLength];
        buffer.get(idBytes);
        String entityId = new String(idBytes, StandardCharsets.UTF_8);

        double posX = buffer.getDouble();
        double posY = buffer.getDouble();
        double dirX = buffer.getDouble();
        double dirY = buffer.getDouble();
        long timestamp = buffer.getLong();

        PacketEntityPosition packet = new PacketEntityPosition(entityId, posX, posY, dirX, dirY);
        packet.timestamp = timestamp;
        return packet;
    }

    public static byte[] serializeBulk(PacketPositionsBulk bulk) {
        // Calcul taille
        int count = bulk.ids.size();
        int size = 1 + Integer.BYTES; // type + count

        for (int i = 0; i < count; i++) {
            String id = bulk.ids.get(i);
            byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
            size += Integer.BYTES;           // id length
            size += idBytes.length;          // id bytes
            size += Double.BYTES;            // x
            size += Double.BYTES;            // y
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte) 2);
        buffer.putInt(count);

        for (int i = 0; i < count; i++) {
            String id = bulk.ids.get(i);
            byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);

            buffer.putInt(idBytes.length);
            buffer.put(idBytes);
            buffer.putDouble(bulk.x.get(i));
            buffer.putDouble(bulk.y.get(i));
        }

        return buffer.array();
    }

    public static PacketPositionsBulk deserializeBulkPositions(ByteBuffer buffer) {

        int count = buffer.getInt();
        System.out.println("LOG DESERIALIZER, count : " + count);
        System.out.println("LIMIT : " + buffer.limit());

        List<String> ids = new ArrayList<>(count);
        List<Double> x = new ArrayList<>(count);
        List<Double> y = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            System.out.println("BOUCLE : " + i);
            System.out.println("REMAINING : " + buffer.remaining());

            int idLen = buffer.getInt();

            if (buffer.remaining() < idLen + 16) { // id + 2 doubles
                throw new BufferUnderflowException();
            }

            byte[] idBytes = new byte[idLen];
            buffer.get(idBytes);
            String id = new String(idBytes, StandardCharsets.UTF_8);

            double px = buffer.getDouble();
            double py = buffer.getDouble();

            ids.add(id);
            x.add(px);
            y.add(py);
        }

        PacketPositionsBulk bulk = new PacketPositionsBulk(new ArrayList<Entity>());
        bulk.ids = ids;
        bulk.x = x;
        bulk.y = y;

        return bulk;
    }
}