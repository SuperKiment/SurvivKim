package com.superkiment.server;

import com.superkiment.common.packets.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TCPServer {

    private final int port;
    private final GameServer gameServer;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private boolean running = false;

    public TCPServer(int port, GameServer gameServer) {
        this.port = port;
        this.gameServer = gameServer;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Serveur TCP démarré sur le port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientConnection client = new ClientConnection(clientSocket, gameServer);
                    threadPool.execute(client);

                    System.out.println("Nouvelle connexion TCP: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}