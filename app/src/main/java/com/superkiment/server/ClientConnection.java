package com.superkiment.server;

import com.superkiment.common.packets.*;
import java.io.*;
import java.net.*;

public class ClientConnection implements Runnable {

    private final Socket socket;
    private final GameServer gameServer;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public String playerId;
    public String playerName;
    private int udpPort;

    public ClientConnection(Socket socket, GameServer gameServer) {
        this.socket = socket;
        this.gameServer = gameServer;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Recevoir les packets
            while (!socket.isClosed()) {
                try {
                    Packet packet = (Packet) in.readObject();
                    Network.handleTCPPacket(packet, this);
                } catch (EOFException | SocketException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public void sendTCP(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InetAddress getAddress() {
        return socket.getInetAddress();
    }

    public void setUdpPort(int port) {
        this.udpPort = port;
    }

    public int getUdpPort() {
        return udpPort;
    }

    private void disconnect() {
        try {
            socket.close();
            gameServer.removeClient(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}