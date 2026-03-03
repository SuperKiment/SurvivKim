package com.superkiment.common;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Classe sans instance qui permet le calcul de temps entre deux loops.
 */
public class Time {

    private static long frameLastTime;
    private static float frameDeltaTime;

    /**
     * In nanoseconds
     */
    private long lastTime;
    /**
     * In seconds
     */
    private float deltaTime;

    /**
     * In nanoseconds
     */
    private long timerStartTime;
    /**
     * In nanoseconds
     */
    private float timerTimeUntilEnd;

    public Time() {
        lastTime = System.nanoTime();
    }

    public static void UpdateFrameTime() {
        long currentTime = System.nanoTime();
        // Conversion en secondes
        frameDeltaTime = (currentTime - frameLastTime) * 1e-9f;
        frameLastTime = currentTime;
    }

    public static float GetDeltaFrameTime() {
        return frameDeltaTime;
    }

    public void updateDeltaTime() {
        long currentTime = System.nanoTime();
        // Conversion en secondes
        deltaTime = (currentTime - lastTime) * 1e-9f;
        lastTime = currentTime;
    }

    public boolean updateTimer() {
        if (System.nanoTime() - timerStartTime > timerTimeUntilEnd) {
            timerStartTime = System.nanoTime();
            return true;
        }

        return false;
    }

    /**
     *
     * @param timeToEnd in milliseconds
     */
    public void setTimer(float timeToEnd) {
        timerStartTime = System.nanoTime();
        timerTimeUntilEnd = timeToEnd * 1_000_000f;
    }

    public float getDeltaTime() {
        return deltaTime;
    }

    /**
     * @return a formatted timstamp, example : "2026-03-03T18:05:37.281Z"
     */
    public static String GetNowTimestampFormatted() {
        return TransformTimestampFormatted(System.currentTimeMillis());
    }

    /**
     * @param timestamp from System.currentTimeMillis()
     * @return a formatted timstamp, example : "2026-03-03T18:05:37.281Z"
     */
    public static String TransformTimestampFormatted(long timestamp) {
        return DateTimeFormatter
                .ISO_INSTANT
                .format(Instant.ofEpochMilli(timestamp));
    }

}
