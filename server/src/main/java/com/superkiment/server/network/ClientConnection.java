package com.superkiment.server.network;

import com.superkiment.common.net.Connection;
import com.superkiment.common.net.Packet;
import com.superkiment.server.GameServer;
import com.superkiment.server.monitor.ServerMonitor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * La liaison entre le client TCP/UDP et le Player actif dans le monde.
 */
public class ClientConnection implements Runnable {

    private final Connection connection;

    public String playerId;
    public String playerName;
    private int udpPort;

    private long lastHeartbeatTime = System.currentTimeMillis();
    private long lastCalculatedPing = 0;

    public ClientConnection(Socket socket) throws IOException {
        this.connection = new Connection(socket);
    }

    @Override
    public void run() {
        connection.listen(packet -> {
            ServerMonitor.getInstance().logTCPReceived(packet);
            Network.ROUTER.post(packet, this);
        });
        Network.ROUTER.post(() -> GameServer.entitiesManager.removeClient(this));
    }

    public void send(Packet packet) {
        connection.send(packet);
    }

    public InetAddress getAddress() {
        return connection.getAddress();
    }

    public void setUdpPort(int port) {
        this.udpPort = port;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void disconnect() {
        connection.close();
    }

    public void onHeartbeat(long clientTimestamp) {
        long now = System.currentTimeMillis();
        lastHeartbeatTime = now;
        lastCalculatedPing = (now - clientTimestamp) * 2;
    }

    public long timeSinceLastHeartbeat(long now) {
        return now - lastHeartbeatTime;
    }
}
