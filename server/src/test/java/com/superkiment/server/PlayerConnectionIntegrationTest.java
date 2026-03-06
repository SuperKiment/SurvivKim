package com.superkiment.server;

import com.superkiment.common.blocks.BlocksManager;
import com.superkiment.common.entities.EntityFactory;
import com.superkiment.common.packets.PacketPlayerJoin;
import com.superkiment.server.entities.ServerEntitiesManager;
import com.superkiment.server.network.ClientConnection;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration E2E — connexion TCP réelle.
 *
 * Architecture du test :
 *   [ClientSocket] <── loopback ──> [ServerSocket -> ClientConnection thread]
 *
 * IMPORTANT : Network.entitiesManager est static final — impossible à remplacer
 * en Java 21. On utilise donc UNE SEULE instance de ServerEntitiesManager pour
 * toute la classe de test (créée dans @BeforeAll, avant le chargement de Network)
 * et on vide simplement ses maps entre chaque test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlayerConnectionIntegrationTest {

    private static ServerSocket serverSocket;
    private static ExecutorService executor;

    private Socket clientSocket;
    private Socket acceptedSocket;
    private ObjectOutputStream clientOut;
    private ObjectInputStream clientIn;

    @BeforeAll
    static void globalSetup() throws Exception {
        // Doit se faire AVANT tout chargement de la classe Network (lazy loading),
        // sans quoi Network.entitiesManager serait null.
        GameServer.entitiesManager = new ServerEntitiesManager();
        GameServer.blocksManager   = new BlocksManager();

        Field f = EntityFactory.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
        EntityFactory.CreateInstance(GameServer.entitiesManager);

        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "test-server-thread");
            t.setDaemon(true);
            return t;
        });
    }

    @BeforeEach
    void setUp() throws Exception {
        // Vider l'état de l'instance EXISTANTE plutôt que d'en créer une nouvelle.
        // Network.entitiesManager est static final : on ne peut pas le remplacer.
        GameServer.entitiesManager.getClients().clear();
        GameServer.entitiesManager.getEntities().clear();

        serverSocket  = new ServerSocket(0);
        clientSocket  = new Socket("localhost", serverSocket.getLocalPort());
        acceptedSocket = serverSocket.accept();

        executor.submit(new ClientConnection(acceptedSocket)::run);

        clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        clientOut.flush();
        clientIn  = new ObjectInputStream(clientSocket.getInputStream());
    }

    @AfterEach
    void tearDown() throws Exception {
        try { clientOut.close();    } catch (Exception ignored) {}
        try { clientIn.close();     } catch (Exception ignored) {}
        try { clientSocket.close(); } catch (Exception ignored) {}
        try { acceptedSocket.close(); } catch (Exception ignored) {}
        try { serverSocket.close(); } catch (Exception ignored) {}
    }

    @AfterAll
    static void globalTearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    // ─────────────────────────── Tests ───────────────────────────────────

    /**
     * Un joueur envoie PacketPlayerJoin -> le serveur doit l'enregistrer dans
     * entitiesManager.getClients() avec le bon playerId et playerName.
     */
    @Test
    @Order(1)
    void playerJoin_registersClientInEntitiesManager() throws Exception {
        String playerId   = "test-player-uuid-001";
        String playerName = "TestPlayer";

        clientOut.writeObject(new PacketPlayerJoin(playerId, playerName));
        clientOut.flush();

        long deadline = System.currentTimeMillis() + 500;
        while (!GameServer.entitiesManager.getClients().containsKey(playerId)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertTrue(GameServer.entitiesManager.getClients().containsKey(playerId),
                "Le joueur doit être enregistré dans entitiesManager.getClients()");

        ClientConnection registered = GameServer.entitiesManager.getClients().get(playerId);
        assertEquals(playerName, registered.playerName,
                "Le nom du joueur doit correspondre");
    }

    /**
     * Deux joueurs rejoignent le serveur -> les deux doivent apparaître dans la map clients.
     */
    @Test
    @Order(2)
    void twoPlayersJoin_bothRegistered() throws Exception {
        clientOut.writeObject(new PacketPlayerJoin("player-A", "Alice"));
        clientOut.flush();

        Socket client2   = new Socket("localhost", serverSocket.getLocalPort());
        Socket accepted2 = serverSocket.accept();
        executor.submit(new ClientConnection(accepted2)::run);

        ObjectOutputStream out2 = new ObjectOutputStream(client2.getOutputStream());
        out2.flush();
        new ObjectInputStream(client2.getInputStream());

        out2.writeObject(new PacketPlayerJoin("player-B", "Bob"));
        out2.flush();

        long deadline = System.currentTimeMillis() + 500;
        while (GameServer.entitiesManager.getClients().size() < 2
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        try {
            assertTrue(GameServer.entitiesManager.getClients().containsKey("player-A"), "Alice doit être enregistrée");
            assertTrue(GameServer.entitiesManager.getClients().containsKey("player-B"), "Bob doit être enregistré");
        } finally {
            out2.close();
            client2.close();
            accepted2.close();
        }
    }

    /**
     * Après déconnexion, le client doit être retiré de la map clients.
     */
    @Test
    @Order(3)
    void playerDisconnects_removedFromClients() throws Exception {
        String playerId = "player-disconnect-test";

        clientOut.writeObject(new PacketPlayerJoin(playerId, "Ephemeral"));
        clientOut.flush();

        long deadline = System.currentTimeMillis() + 500;
        while (!GameServer.entitiesManager.getClients().containsKey(playerId)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(GameServer.entitiesManager.getClients().containsKey(playerId),
                "Le joueur doit d'abord apparaître");

        clientSocket.close();

        deadline = System.currentTimeMillis() + 1000;
        while (GameServer.entitiesManager.getClients().containsKey(playerId)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        assertFalse(GameServer.entitiesManager.getClients().containsKey(playerId),
                "Le joueur doit être retiré de la map après déconnexion");
    }
}