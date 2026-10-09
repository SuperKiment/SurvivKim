package com.superkiment.server;

import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.entities.Player;
import com.superkiment.common.net.Connection;
import com.superkiment.common.net.EntitySync;
import com.superkiment.common.net.Packet;
import com.superkiment.common.net.Packets.*;
import com.superkiment.server.entities.ServerEntitiesManager;
import com.superkiment.server.network.ClientConnection;
import com.superkiment.server.network.Network;
import org.joml.Vector2d;
import org.junit.jupiter.api.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration E2E — connexion TCP réelle.
 * <p>
 * [Connection client] <── loopback ──> [ServerSocket -> ClientConnection thread]
 * <p>
 * Les packets reçus par le serveur sont mis en file : {@link #await} joue le rôle de la boucle de tick en vidant
 * {@link Network#ROUTER}.
 */
public class PlayerConnectionIntegrationTest {

    private static ExecutorService executor;
    private ServerSocket serverSocket;

    /**
     * Un faux client : sa connexion et tout ce qu'il a reçu.
     */
    private record TestClient(Connection connection, List<Packet> received) {
        void send(Packet p) {
            connection.send(p);
        }

        boolean hasReceived(Class<?> type) {
            return received.stream().anyMatch(type::isInstance);
        }
    }

    @BeforeAll
    static void globalSetup() {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "test-server-thread");
            t.setDaemon(true);
            return t;
        });
    }

    @BeforeEach
    void setUp() throws Exception {
        GameServer.entitiesManager = new ServerEntitiesManager();
        GameServer.blocksManager = new BlocksManager();
        serverSocket = new ServerSocket(0);
    }

    @AfterEach
    void tearDown() throws Exception {
        serverSocket.close();
    }

    @AfterAll
    static void globalTearDown() throws Exception {
        executor.shutdownNow();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    private TestClient connect() throws Exception {
        Socket socket = new Socket("localhost", serverSocket.getLocalPort());
        executor.submit(new ClientConnection(serverSocket.accept()));

        TestClient client = new TestClient(new Connection(socket), new CopyOnWriteArrayList<>());
        executor.submit(() -> client.connection().listen(client.received()::add));
        return client;
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < deadline) {
            Network.ROUTER.drain();
            if (condition.getAsBoolean()) return;
            Thread.sleep(10);
        }
        fail("Condition non atteinte dans le délai");
    }

    // ─────────────────────────── Tests ───────────────────────────────────

    @Test
    void playerJoin_registersClientInEntitiesManager() throws Exception {
        connect().send(new Join("test-player-uuid-001", "TestPlayer"));

        await(() -> GameServer.entitiesManager.getClients().containsKey("test-player-uuid-001"));

        assertEquals("TestPlayer", GameServer.entitiesManager.getClients().get("test-player-uuid-001").playerName);
    }

    @Test
    void twoPlayersJoin_bothRegistered() throws Exception {
        connect().send(new Join("player-A", "Alice"));
        connect().send(new Join("player-B", "Bob"));

        await(() -> GameServer.entitiesManager.getClients().size() == 2);
    }

    @Test
    void playerDisconnects_removedFromClientsAndWorld() throws Exception {
        TestClient client = connect();
        Player player = new Player(new Vector2d());
        player.id = "player-disconnect-test";
        client.send(new Join(player.id, "Ephemeral"));
        client.send(EntitySync.spawnPacket(player));

        await(() -> GameServer.entitiesManager.getEntityFromID(player.id) != null);

        client.connection().close();

        await(() -> !GameServer.entitiesManager.getClients().containsKey(player.id)
                && GameServer.entitiesManager.getEntityFromID(player.id) == null);
    }

    @Test
    void entitySpawnedByOneClient_isReplicatedToOthers_andSyncFieldsFollow() throws Exception {
        TestClient alice = connect();
        TestClient bob = connect();
        alice.send(new Join("alice", "Alice"));
        bob.send(new Join("bob", "Bob"));
        await(() -> GameServer.entitiesManager.getClients().size() == 2);

        Player player = new Player(new Vector2d(10, 20));
        alice.send(EntitySync.spawnPacket(player));
        await(() -> bob.hasReceived(SpawnEntity.class));

        // Modification côté serveur : aucun code réseau, juste un champ @Sync modifié
        GameServer.entitiesManager.getEntityFromID(player.id).hp = 42;
        GameServer.entitiesManager.broadcastChanges();

        await(() -> bob.received().stream().anyMatch(p ->
                p instanceof UpdateEntity u && u.id().equals(player.id) && Float.valueOf(42f).equals(u.changes().get("hp"))));
    }
}
