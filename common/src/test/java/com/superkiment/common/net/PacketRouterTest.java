package com.superkiment.common.net;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PacketRouterTest {

    @Test
    void post_isDeferredUntilDrain() {
        List<String> received = new ArrayList<>();
        PacketRouter<String> router = new PacketRouter<String>()
                .on(Packets.Join.class, (p, ctx) -> received.add(ctx + ":" + p.playerName()));

        router.post(new Packets.Join("id", "Alice"), "conn1");
        assertTrue(received.isEmpty(), "Rien ne doit s'exécuter hors du thread de jeu");

        router.drain();
        assertEquals(List.of("conn1:Alice"), received);
    }

    @Test
    void dispatch_routesByClass_andIgnoresUnknown() {
        List<Object> received = new ArrayList<>();
        PacketRouter<Void> router = new PacketRouter<Void>()
                .on(Packets.Heartbeat.class, (p, v) -> received.add(p));

        router.dispatch(new Packets.Heartbeat(1), null);
        router.dispatch(new Packets.RemoveEntity("x"), null);

        assertEquals(List.of(new Packets.Heartbeat(1)), received);
    }

    @Test
    void drain_survivesHandlerException() {
        List<Integer> ran = new ArrayList<>();
        PacketRouter<Void> router = new PacketRouter<>();
        router.post(() -> { throw new IllegalStateException("boom"); });
        router.post(() -> ran.add(1));

        router.drain();
        assertEquals(List.of(1), ran);
    }
}
