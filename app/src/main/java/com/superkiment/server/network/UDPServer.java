package com.superkiment.server.network;

import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.packets.*;
import com.superkiment.common.packets.entity.PacketEntityPosition;

import java.io.IOException;
import java.net.*;
import java.util.List;

public class UDPServer {

    private final int port;
    public DatagramSocket socket;
    private boolean running = false;

    public UDPServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            socket = new DatagramSocket(port);

            running = true;
            System.out.println("Serveur UDP démarré sur le port " + socket.getLocalPort());

            byte[] buffer = new byte[1024];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    // Désérialiser le packet
                    PacketEntityPosition posPacket = PacketSerializer.deserializePositionUDP(
                            packet.getData()
                    );

                    // Traiter le packet
                    Network.handleUDPPacket(posPacket, packet.getAddress(), packet.getPort(), this);

                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void sendPosition(PacketEntityPosition packet, InetAddress address, int port) {
        try {
            byte[] data = PacketSerializer.serializePositionUDP(packet);
            DatagramPacket datagramPacket = new DatagramPacket(
                    data, data.length, address, port
            );
            socket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}