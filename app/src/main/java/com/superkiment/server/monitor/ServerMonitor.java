package com.superkiment.server.monitor;

import com.superkiment.common.packets.Packet;
import com.superkiment.common.packets.PacketEntityPosition;
import com.superkiment.common.packets.PacketSerializer;
import com.superkiment.server.GameServer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.text.SimpleDateFormat;

/**
 * Moniteur central qui collecte toutes les stats du serveur
 */
public class ServerMonitor {

    private static ServerMonitor instance;

    // Stats
    private int totalTCPPackets = 0;
    private int totalUDPPackets = 0;
    private long totalBytesReceived = 0;
    private long totalBytesSent = 0;
    private int connectedClients = 0;
    private int totalEntities = 0;

    // Logs récents (max 100)
    private final Queue<LogEntry> recentLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOGS = 100;

    // Stats par seconde
    private int tcpPacketsPerSecond = 0;
    private int udpPacketsPerSecond = 0;
    private long bytesPerSecond = 0;

    private long lastResetTime = System.currentTimeMillis();

    private ServerMonitor() {}

    public static ServerMonitor getInstance() {
        if (instance == null) {
            instance = new ServerMonitor();
        }
        return instance;
    }

    /**
     * Logger un événement
     */
    public void log(String type, String message) {
        LogEntry entry = new LogEntry(type, message);
        recentLogs.offer(entry);

        // Limiter à 100 logs
        while (recentLogs.size() > MAX_LOGS) {
            recentLogs.poll();
        }

        // Notifier les dashboards connectés
        MonitorWebServer.broadcast(getStatsJSON());
    }

    /**
     * Logger un packet TCP reçu
     */
    public void logTCPReceived(Packet packet) throws IOException {
        byte[] data = PacketSerializer.serialize(packet);
        int bytes = data.length;
        String packetType = packet.getType().toString();

        totalTCPPackets++;
        tcpPacketsPerSecond++;
        totalBytesReceived += bytes;
        bytesPerSecond += bytes;
        log("TCP_IN", "← " + packetType + " (" + bytes + " bytes)");
    }

    /**
     * Logger un packet TCP envoyé
     */
    public void logTCPSent(Packet packet) throws IOException {
        byte[] data = PacketSerializer.serialize(packet);
        int bytes = data.length;
        String packetType = packet.getType().toString();

        totalTCPPackets++;
        tcpPacketsPerSecond++;
        totalBytesSent += bytes;
        bytesPerSecond += bytes;
        log("TCP_OUT", "→ " + packetType + " (" + bytes + " bytes)");
    }

    /**
     * Logger un packet UDP reçu
     */
    public void logUDPReceived(PacketEntityPosition packet) {
        byte[] data = PacketSerializer.serializePositionUDP(packet);
        int bytes = data.length;
        String entityId = packet.entityId;

        totalUDPPackets++;
        udpPacketsPerSecond++;
        totalBytesReceived += bytes;
        bytesPerSecond += bytes;
        //log("UDP_IN", "← Position " + entityId.substring(0, 8) + "... (" + bytes + " bytes)");
    }

    /**
     * Logger un packet UDP envoyé
     */
    public void logUDPSent(PacketEntityPosition packet) {
        byte[] data = PacketSerializer.serializePositionUDP(packet);
        int bytes = data.length;
        String entityId = packet.entityId;

        totalUDPPackets++;
        udpPacketsPerSecond++;
        totalBytesSent += bytes;
        bytesPerSecond += bytes;
        //log("UDP_OUT", "→ Position " + entityId.substring(0, 8) + "... (" + bytes + " bytes)");
    }

    /**
     * Mettre à jour le nombre de clients
     */
    public void setConnectedClients(int count) {
        this.connectedClients = count;
    }

    /**
     * Mettre à jour le nombre d'entités
     */
    public void setTotalEntities(int count) {
        this.totalEntities = count;
    }

    /**
     * Réinitialiser les stats par seconde
     */
    public void resetPerSecondStats() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime >= 1000) {
            tcpPacketsPerSecond = 0;
            udpPacketsPerSecond = 0;
            bytesPerSecond = 0;
            lastResetTime = now;
        }
    }

    /**
     * Obtenir les stats en JSON
     */
    public String getStatsJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"totalTCPPackets\":").append(totalTCPPackets).append(",");
        json.append("\"totalUDPPackets\":").append(totalUDPPackets).append(",");
        json.append("\"totalBytesReceived\":").append(totalBytesReceived).append(",");
        json.append("\"totalBytesSent\":").append(totalBytesSent).append(",");
        json.append("\"connectedClients\":").append(connectedClients).append(",");
        json.append("\"totalEntities\":").append(totalEntities).append(",");
        json.append("\"tcpPacketsPerSecond\":").append(tcpPacketsPerSecond).append(",");
        json.append("\"udpPacketsPerSecond\":").append(udpPacketsPerSecond).append(",");
        json.append("\"bytesPerSecond\":").append(bytesPerSecond).append(",");
        json.append("\"recentLogs\":[");

        List<LogEntry> logs = new ArrayList<>(recentLogs);
        for (int i = 0; i < logs.size(); i++) {
            json.append(logs.get(i).toJSON());
            if (i < logs.size() - 1) json.append(",");
        }

        json.append("]}");
        return json.toString();
    }

    public void statsUpdateLoop(GameServer gameServer) {
        while (gameServer.running) {
            try {
                Thread.sleep(1000); // Toutes les secondes

                // Mettre à jour les stats
                setConnectedClients(GameServer.entitiesManager.getClients().size());
                setTotalEntities(GameServer.entitiesManager.getEntities().size());
                resetPerSecondStats();

                // Broadcaster aux dashboards
                MonitorWebServer.broadcast(getStatsJSON());

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Classe pour les entrées de log
     */
    public static class LogEntry {
        private final String timestamp;
        private final String type;
        private final String message;

        public LogEntry(String type, String message) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            this.timestamp = sdf.format(new Date());
            this.type = type;
            this.message = message;
        }

        public String toJSON() {
            return String.format("{\"timestamp\":\"%s\",\"type\":\"%s\",\"message\":\"%s\"}",
                    timestamp, type, message.replace("\"", "\\\""));
        }
    }
}