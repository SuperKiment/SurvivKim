package com.superkiment.client.network;

import com.superkiment.client.Main;
import com.superkiment.common.Logger;
import com.superkiment.common.Time;
import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.entities.EntitiesManager;
import com.superkiment.common.entities.Entity;
import com.superkiment.common.entities.Player;
import com.superkiment.common.net.*;
import com.superkiment.common.net.Packets.*;
import org.joml.Vector2d;

import java.net.Socket;
import java.util.Map;
import java.util.UUID;

import static com.superkiment.client.graphics.ui.dynamic_ui.ShapeModel_UI.AddDynamicUIElementsToEntity;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;

/**
 * Client unique pour la réception et l'envoi des données.
 * <p>
 * Usage côté gameplay : {@link #spawn(Entity)} pour créer une entité dans le monde partagé,
 * {@link #send(Packet)} pour tout autre packet. Les champs @Sync du joueur local sont propagés automatiquement.
 */
public class GameClient {

    public static final String SERVER_ADDRESS = "localhost";
    public static final int TCP_PORT = 56201;
    public static final int UDP_PORT = 56202;

    private static final float POSITION_SEND_RATE = 1.0f / 30.0f; // 30 Hz
    private static final float HEARTBEAT_INTERVAL_MILLIS = 4_000f;

    private final String serverAddress;
    private final int tcpPort;
    private final int udpPort;

    private Connection tcp;
    private UDPClient udpClient;

    private final EntitiesManager entitiesManager = Main.entitiesManager;
    private final BlocksManager blocksManager = Main.blocksManager;

    private final EntitySync.Tracker tracker = new EntitySync.Tracker();
    private final Time heartbeatTimer = new Time();
    private float positionSendTimer = 0;

    private Player localPlayer;
    private String playerId;

    private boolean connected = false;

    /**
     * Toutes les routes TCP du client. Exécutées sur le thread de rendu via {@link #update(float)}.
     */
    private final PacketRouter<Void> router = new PacketRouter<Void>()
            .on(Join.class, (p, v) -> {
                if (!p.playerId().equals(playerId)) Logger.info("Joueur rejoint: " + p.playerName());
            })
            .on(SpawnEntity.class, (p, v) -> onSpawn(p))
            .on(UpdateEntity.class, (p, v) -> onUpdate(p))
            .on(RemoveEntity.class, (p, v) -> entitiesManager.removeEntity(p.id()))
            .on(CreateBlock.class, (p, v) -> blocksManager.addBlock(new Vector2d(p.x(), p.y())));

    public GameClient(String serverAddress, int tcpPort, int udpPort) {
        this.serverAddress = serverAddress;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        heartbeatTimer.setTimer(HEARTBEAT_INTERVAL_MILLIS);
    }

    private boolean connect(String playerName) {
        try {
            playerId = UUID.randomUUID().toString();

            tcp = new Connection(new Socket(serverAddress, tcpPort));
            Thread receive = new Thread(() -> tcp.listen(p -> router.post(p, null)), "tcp-receive-loop");
            receive.setDaemon(true);
            receive.start();

            udpClient = new UDPClient(serverAddress, udpPort, this);
            udpClient.connect();

            localPlayer = new Player(new Vector2d(400, 300));
            localPlayer.id = playerId;
            localPlayer.name = playerName;

            send(new Join(playerId, playerName));
            entitiesManager.addEntity(localPlayer);
            spawn(localPlayer);

            connected = true;
            Logger.info("Connecté au serveur en tant que " + playerName);
            return true;
        } catch (Exception e) {
            Logger.error("Connexion impossible", e);
            return false;
        }
    }

    /**
     * Tentative de connexion au serveur.
     */
    public static GameClient tryConnectToServer(long window) {
        Logger.info("Tentative de connexion au serveur...");
        GameClient gameClient = new GameClient(SERVER_ADDRESS, TCP_PORT, UDP_PORT);

        if (gameClient.connect("Player_" + System.currentTimeMillis() % 1000)) {
            glfwSetWindowTitle(window, "Mon Jeu Multijoueur - Connecté");
            return gameClient;
        }

        Logger.warn("Échec de connexion au serveur");
        glfwSetWindowTitle(window, "Mon Jeu Multijoueur - Déconnecté");
        return null;
    }

    /**
     * À appeler à chaque frame : traite les packets reçus et envoie l'état local.
     */
    public void update(float deltaTime) {
        router.drain();

        if (heartbeatTimer.updateTimer()) send(new Heartbeat(System.currentTimeMillis()));

        Map<String, Object> changes = tracker.diff(localPlayer);
        if (changes != null) send(new UpdateEntity(localPlayer.id, changes));

        positionSendTimer += deltaTime;
        if (positionSendTimer >= POSITION_SEND_RATE) {
            udpClient.sendPosition(new PacketEntityPosition(localPlayer.id,
                    localPlayer.pos.x, localPlayer.pos.y,
                    localPlayer.dirLookTarget.x, localPlayer.dirLookTarget.y));
            positionSendTimer = 0;
        }
    }

    public void send(Packet packet) {
        tcp.send(packet);
    }

    /**
     * Fait apparaître l'entité chez le serveur et tous les clients (y compris celui-ci).
     */
    public void spawn(Entity entity) {
        send(EntitySync.spawnPacket(entity));
    }

    private void onSpawn(SpawnEntity packet) {
        if (entitiesManager.getEntityFromID(packet.id()) != null) return; // déjà connue (ex : notre joueur)

        Entity entity = EntitySync.create(packet);
        AddDynamicUIElementsToEntity(entity);
        entitiesManager.addEntity(entity);
        Logger.debug("Entité distante créée: " + entity);
    }

    private void onUpdate(UpdateEntity packet) {
        Entity entity = entitiesManager.getEntityFromID(packet.id());
        if (entity == null) return;

        EntitySync.apply(entity, packet.changes());
        if (entity == localPlayer) tracker.rebase(entity); // ne pas renvoyer l'état reçu en écho
    }

    /**
     * Position d'une entité unique (UDP).
     */
    public void handleUDPPositionPacket(PacketEntityPosition packet) {
        if (packet.entityId.equals(playerId)) return;

        router.post(() -> {
            Entity entity = entitiesManager.getEntityFromID(packet.entityId);
            if (entity != null) {
                entity.pos.set(packet.posX, packet.posY);
                entity.dirLookTarget.set(packet.dirX, packet.dirY);
            }
        });
    }

    /**
     * Positions de plusieurs entités (UDP).
     */
    public void handleUDPBulkPositionPacket(PacketPositionsBulk packet) {
        router.post(() -> {
            for (int i = 0; i < packet.ids.size(); i++) {
                Entity entity = entitiesManager.getEntityFromID(packet.ids.get(i));
                if (entity != null) entity.pos.set(packet.x.get(i), packet.y.get(i));
            }
        });
    }

    public void disconnect() {
        connected = false;
        if (tcp != null) tcp.close();
        if (udpClient != null) udpClient.disconnect();
    }

    public Player getLocalPlayer() {
        return localPlayer;
    }

    public boolean isConnected() {
        return connected && !tcp.isClosed();
    }
}
