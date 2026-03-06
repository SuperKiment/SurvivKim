package com.superkiment.server.network;

import com.superkiment.common.Logger;
import com.superkiment.server.GameServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class TCPServer {

    private final int port;
    private final GameServer gameServer;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private boolean running = false;

    public TCPServer(int port, GameServer gameServer) {
        this.port = port;
        this.gameServer = gameServer;
        this.threadPool = Executors.newCachedThreadPool(
                new ThreadFactory() {
                    private int counter = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("tcp-client-" + (++counter));
                        t.setDaemon(true);
                        return t;
                    }
                }
        );
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            Logger.log(Logger.LogLevel.INFO, "Serveur TCP démarré sur le port " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientConnection client = new ClientConnection(clientSocket);
                    threadPool.execute(client);

                    Logger.log(Logger.LogLevel.INFO, "Nouvelle connexion TCP: " + clientSocket.getInetAddress());
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