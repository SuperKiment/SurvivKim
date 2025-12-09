package com.superkiment.client.network;

import com.superkiment.common.packets.*;

import java.io.*;
import java.net.Socket;

public class TCPClient {

    private final String serverAddress;
    private final int port;
    private final GameClient gameClient;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread receiveThread;

    public TCPClient(String serverAddress, int port, GameClient gameClient) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.gameClient = gameClient;
    }

    public void connect() throws IOException {
        socket = new Socket(serverAddress, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // Thread pour recevoir les packets
        receiveThread = new Thread(this::receiveLoop);
        receiveThread.start();

        System.out.println("Connecté au serveur TCP: " + serverAddress + ":" + port);
    }

    private void receiveLoop() {
        try {
            while (!socket.isClosed()) {
                try {
                    Packet packet = (Packet) in.readObject();
                    System.out.println("Packet arriving : " + packet.getClass().getName());
                    gameClient.handleTCPPacket(packet);
                } catch (EOFException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("Connexion TCP fermée");
        }
    }

    public void send(Packet packet) {
        try {
            System.out.println("TCP SENT : " + packet);
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}