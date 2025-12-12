package com.superkiment.server;

import com.superkiment.common.Time;
import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.server.monitor.MonitorWebServer;
import com.superkiment.server.monitor.ServerMonitor;
import com.superkiment.server.network.Network;
import com.superkiment.server.network.TCPServer;
import com.superkiment.server.network.UDPServer;

public class GameServer {

    private static final int TCP_PORT = 7777;
    private static final int UDP_PORT = 7778;
    private static final int TICK_RATE = 20; // 20 ticks par seconde

    private TCPServer tcpServer;
    private UDPServer udpServer;
    private ServerMonitor monitor;

    public static EntitiesManager entitiesManager;
    public static BlocksManager blocksManager;

    public boolean running = false;

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }

    public void start() {
        System.out.println("=== Démarrage du serveur ===");
        System.out.println("TCP Port: " + TCP_PORT);
        System.out.println("UDP Port: " + UDP_PORT);

        monitor = ServerMonitor.getInstance();
        MonitorWebServer monitorServer = new MonitorWebServer();
        monitorServer.start();

        entitiesManager = new EntitiesManager();
        blocksManager = new BlocksManager();

        //Network.setupNetwork(entitiesManager);

        running = true;

        // Démarrer les serveurs TCP et UDP
        tcpServer = new TCPServer(TCP_PORT, this);
        new Thread(tcpServer::start).start();

        udpServer = new UDPServer(UDP_PORT);
        new Thread(udpServer::start).start();

        //Mise à jour des stats
        new Thread(() -> monitor.statsUpdateLoop(this)).start();

        loop();
    }

    /**
     * Boucle principale du serveur.
     */
    private void loop() {
        long lastTime = System.nanoTime();

        // = nano-seconds Per Tick
        double nsPerTick = 1_000_000_000.0 / TICK_RATE;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            if (delta >= 1) {
                tick();
                delta--;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Une loop du jeu en lui-même
     */
    private void tick() {
        Time.updateDeltaTime();

        for (Entity entity : entitiesManager.getEntities().values()) {
            entity.updateLogic(entitiesManager, blocksManager);
        }
        //System.out.println(entitiesManager.getEntities().size());

        Network.broadcastBulkPositionUDP(entitiesManager, udpServer);
    }

    public void stop() {
        running = false;
        tcpServer.stop();
        udpServer.stop();
    }
}