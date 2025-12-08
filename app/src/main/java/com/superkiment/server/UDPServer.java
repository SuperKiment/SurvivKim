package com.superkiment.server;

import com.superkiment.common.packets.*;
import java.io.IOException;
import java.net.*;

public class UDPServer {

    private final int port;
    private final GameServer gameServer;
    private DatagramSocket socket;
    private boolean running = false;

    public UDPServer(int port, GameServer gameServer) {
        this.port = port;
        this.gameServer = gameServer;
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
                    System.out.println("From client: " + packet.getAddress() + ":" + packet.getPort());

                    // Désérialiser le packet
                    PacketEntityPosition posPacket = PacketSerializer.deserializePositionUDP(
                            packet.getData()
                    );

                    // Traiter le packet
                    gameServer.handleUDPPacket(posPacket, packet.getAddress(), packet.getPort());

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