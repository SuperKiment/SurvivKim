package com.superkiment.server.monitor;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serveur HTTP pour le dashboard
 */
public class MonitorWebServer {

    private static final int PORT = 8080;
    private HttpServer server;
    private static final List<HttpExchange> sseClients = new CopyOnWriteArrayList<>();

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Route pour la page HTML
            server.createContext("/", this::handleDashboard);

            // Route pour le stream de données (SSE)
            server.createContext("/stats", this::handleStatsStream);

            server.setExecutor(null);
            server.start();

            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║  Dashboard disponible sur:             ║");
            System.out.println("║  http://localhost:" + PORT + "                 ║");
            System.out.println("╚════════════════════════════════════════╝");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Servir la page HTML du dashboard
     */
    private void handleDashboard(HttpExchange exchange) throws IOException {
        String html = getDashboardHTML();
        byte[] response = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);

        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    /**
     * Stream SSE (Server-Sent Events) pour les mises à jour temps réel
     */
    private void handleStatsStream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);

        sseClients.add(exchange);

        // Envoyer les stats initiales
        sendSSE(exchange, ServerMonitor.getInstance().getStatsJSON());
    }

    /**
     * Envoyer un message SSE à un client
     */
    private void sendSSE(HttpExchange exchange, String data) {
        try {
            OutputStream os = exchange.getResponseBody();
            String message = "data: " + data + "\n\n";
            os.write(message.getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            sseClients.remove(exchange);
        }
    }

    /**
     * Broadcaster les stats à tous les clients connectés
     */
    public static void broadcast(String data) {
        List<HttpExchange> toRemove = new ArrayList<>();

        for (HttpExchange client : sseClients) {
            try {
                OutputStream os = client.getResponseBody();
                String message = "data: " + data + "\n\n";
                os.write(message.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                toRemove.add(client);
            }
        }

        sseClients.removeAll(toRemove);
    }

    /**
     * Générer le HTML du dashboard
     */
    private String getDashboardHTML() {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Server Monitor</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: #fff;
                            padding: 20px;
                            min-height: 100vh;
                        }
                        .container { max-width: 1400px; margin: 0 auto; }
                        h1 {
                            text-align: center;
                            margin-bottom: 30px;
                            font-size: 2.5em;
                            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
                        }
                        .stats-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                            gap: 20px;
                            margin-bottom: 30px;
                        }
                        .stat-card {
                            background: rgba(255, 255, 255, 0.1);
                            backdrop-filter: blur(10px);
                            border-radius: 15px;
                            padding: 20px;
                            border: 1px solid rgba(255, 255, 255, 0.2);
                            transition: transform 0.3s;
                        }
                        .stat-card:hover { transform: translateY(-5px); }
                        .stat-label {
                            font-size: 0.9em;
                            opacity: 0.8;
                            margin-bottom: 10px;
                        }
                        .stat-value {
                            font-size: 2em;
                            font-weight: bold;
                        }
                        .stat-unit {
                            font-size: 0.7em;
                            opacity: 0.7;
                            margin-left: 5px;
                        }
                        .logs-container {
                            background: rgba(0, 0, 0, 0.3);
                            border-radius: 15px;
                            padding: 20px;
                            max-height: 500px;
                            overflow-y: auto;
                        }
                        .logs-title {
                            font-size: 1.5em;
                            margin-bottom: 15px;
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                        }
                        .log-entry {
                            padding: 10px;
                            margin-bottom: 5px;
                            border-radius: 8px;
                            background: rgba(255, 255, 255, 0.05);
                            border-left: 3px solid;
                            display: flex;
                            gap: 10px;
                            font-family: 'Courier New', monospace;
                            font-size: 0.9em;
                            animation: slideIn 0.3s;
                        }
                        @keyframes slideIn {
                            from { opacity: 0; transform: translateX(-20px); }
                            to { opacity: 1; transform: translateX(0); }
                        }
                        .log-timestamp { opacity: 0.6; min-width: 100px; }
                        .log-type {
                            font-weight: bold;
                            min-width: 80px;
                        }
                        .TCP_IN { border-left-color: #4ade80; }
                        .TCP_OUT { border-left-color: #60a5fa; }
                        .UDP_IN { border-left-color: #fbbf24; }
                        .UDP_OUT { border-left-color: #f97316; }
                        .status-indicator {
                            display: inline-block;
                            width: 10px;
                            height: 10px;
                            border-radius: 50%;
                            background: #4ade80;
                            animation: pulse 2s infinite;
                        }
                        @keyframes pulse {
                            0%, 100% { opacity: 1; }
                            50% { opacity: 0.5; }
                        }
                        .clear-btn {
                            background: rgba(255, 255, 255, 0.2);
                            border: none;
                            color: white;
                            padding: 8px 16px;
                            border-radius: 8px;
                            cursor: pointer;
                            transition: background 0.3s;
                        }
                        .clear-btn:hover { background: rgba(255, 255, 255, 0.3); }
                        ::-webkit-scrollbar { width: 8px; }
                        ::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.2); border-radius: 10px; }
                        ::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.3); border-radius: 10px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1><span class="status-indicator"></span> Server Monitor Dashboard</h1>
               
                        <div class="stats-grid">
                            <div class="stat-card">
                                <div class="stat-label">Clients connectés</div>
                                <div class="stat-value" id="clients">0</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">Entités totales</div>
                                <div class="stat-value" id="entities">0</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">Packets TCP</div>
                                <div class="stat-value" id="tcp">0</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">Packets UDP</div>
                                <div class="stat-value" id="udp">0</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">TCP/s</div>
                                <div class="stat-value" id="tcpPerSec">0<span class="stat-unit">pkt/s</span></div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">UDP/s</div>
                                <div class="stat-value" id="udpPerSec">0<span class="stat-unit">pkt/s</span></div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">Données reçues</div>
                                <div class="stat-value" id="bytesIn">0<span class="stat-unit">KB</span></div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-label">Données envoyées</div>
                                <div class="stat-value" id="bytesOut">0<span class="stat-unit">KB</span></div>
                            </div>
                        </div>
               
                        <div class="logs-container">
                            <div class="logs-title">
                                <span>📋 Logs récents</span>
                                <button class="clear-btn" onclick="clearLogs()">Effacer</button>
                            </div>
                            <div id="logs"></div>
                        </div>
                    </div>
               
                    <script>
                        const eventSource = new EventSource('/stats');
                        const logsContainer = document.getElementById('logs');
                        let autoScroll = true;
               
                        logsContainer.addEventListener('scroll', () => {
                            const isAtBottom = logsContainer.scrollHeight - logsContainer.scrollTop === logsContainer.clientHeight;
                            autoScroll = isAtBottom;
                        });
               
                        eventSource.onmessage = (event) => {
                            const data = JSON.parse(event.data);
               
                            document.getElementById('clients').textContent = data.connectedClients;
                            document.getElementById('entities').textContent = data.totalEntities;
                            document.getElementById('tcp').textContent = data.totalTCPPackets.toLocaleString();
                            document.getElementById('udp').textContent = data.totalUDPPackets.toLocaleString();
                            document.getElementById('tcpPerSec').innerHTML = data.tcpPacketsPerSecond + '<span class="stat-unit">pkt/s</span>';
                            document.getElementById('udpPerSec').innerHTML = data.udpPacketsPerSecond + '<span class="stat-unit">pkt/s</span>';
                            document.getElementById('bytesIn').innerHTML = (data.totalBytesReceived / 1024).toFixed(2) + '<span class="stat-unit">KB</span>';
                            document.getElementById('bytesOut').innerHTML = (data.totalBytesSent / 1024).toFixed(2) + '<span class="stat-unit">KB</span>';
               
                            logsContainer.innerHTML = '';
                            data.recentLogs.reverse().forEach(log => {
                                const logEl = document.createElement('div');
                                logEl.className = 'log-entry ' + log.type;
                                logEl.innerHTML = `
                                    <span class="log-timestamp">${log.timestamp}</span>
                                    <span class="log-type">${log.type}</span>
                                    <span class="log-message">${log.message}</span>
                                `;
                                logsContainer.appendChild(logEl);
                            });
               
                            if (autoScroll) {
                                logsContainer.scrollTop = logsContainer.scrollHeight;
                            }
                        };
               
                        function clearLogs() {
                            if (confirm('Effacer tous les logs ?')) {
                                logsContainer.innerHTML = '<div style="text-align: center; opacity: 0.5; padding: 50px;">Logs effacés</div>';
                            }
                        }
                    </script>
                </body>
                </html>
               """;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}