package com.superkiment.client.network;

import com.superkiment.client.Main;
import com.superkiment.common.Time;
import com.superkiment.common.packets.PacketHeartbeat;

public class Heartbeat {
    public static final float HEARTBEAT_INTERVAL_MILLIS = 4_000f;

    private final Time time;
    int a = 0;

    public Heartbeat() {
        time = new Time();

        // Minuteur de 4 secondes
        time.setTimer(HEARTBEAT_INTERVAL_MILLIS);
    }

    public void update() {
        if (!time.updateTimer()) return;
        Main.gameClient.getTCPClient().send(new PacketHeartbeat(Main.gameClient.getLocalPlayer().id, System.currentTimeMillis()));
    }
}
