package com.superkiment.client.network;

import com.superkiment.common.Time;

public class ClientHeartbeat {
    private final Time time;
    int a = 0;

    public ClientHeartbeat() {
        time = new Time();

        // Minuteur de 4 secondes
        time.setTimer(4_000f);
    }

    public void update() {
        if (!time.updateTimer()) return;

        System.out.println("heartbeat" + a++);
    }
}
