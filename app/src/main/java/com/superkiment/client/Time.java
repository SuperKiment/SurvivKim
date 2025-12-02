package com.superkiment.client;

public class Time {

    private static long lastFrameTime;
    private static float deltaTime;

    public static void updateDeltaTime() {
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastFrameTime)/1_000_000_000f;
        lastFrameTime = currentTime;
    }

    public static float getDeltaTime() {
        return deltaTime;
    }
}
