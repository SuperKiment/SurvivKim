package com.superkiment.client.network;

import com.superkiment.common.packets.*;
import java.io.IOException;
import java.net.*;

public class UDPClient {

    private final String serverAddress;
    private final int port;
    private final GameClient gameClient;

    private DatagramSocket socket;
    private InetAddress serverInetAddress;
    private Thread receiveThread;

    public UDPClient(String serverAddress, int port, GameClient gameClient) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.gameClient = gameClient;
    }

    public void connect() throws IOException {
        socket = new DatagramSocket();
        serverInetAddress = InetAddress.getByName(serverAddress);

        // Thread pour recevoir les packets
        receiveThread = new Thread(this::receiveLoop);
        receiveThread.start();

        System.out.println("Connecté au serveur UDP: " + serverAddress + ":" + port);
    }

    private void receiveLoop() {
        byte[] buffer = new byte[1024];

        while (!socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Désérialiser le packet
                PacketEntityPosition posPacket = PacketSerializer.deserializePositionUDP(
                        packet.getData()
                );

                gameClient.handleUDPPacket(posPacket);

            } catch (IOException e) {
                if (!socket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendPosition(PacketEntityPosition packet) {
        try {
            byte[] data = PacketSerializer.serializePositionUDP(packet);
            DatagramPacket datagramPacket = new DatagramPacket(
                    data, data.length, serverInetAddress, port
            );
            socket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.close();
        }
    }
}