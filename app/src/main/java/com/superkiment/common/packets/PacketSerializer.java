package com.superkiment.common.packets;

import java.io.*;
import java.nio.ByteBuffer;

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
        ByteBuffer buffer = ByteBuffer.allocate(4 + idBytes.length + 8*4 + 8);

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
    public static PacketEntityPosition deserializePositionUDP(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int idLength = buffer.getInt();
        byte[] idBytes = new byte[idLength];
        buffer.get(idBytes);
        String entityId = new String(idBytes);

        double posX = buffer.getDouble();
        double posY = buffer.getDouble();
        double dirX = buffer.getDouble();
        double dirY = buffer.getDouble();
        long timestamp = buffer.getLong();

        PacketEntityPosition packet = new PacketEntityPosition(entityId, posX, posY, dirX, dirY);
        packet.timestamp = timestamp;
        return packet;
    }
}