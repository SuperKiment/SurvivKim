package com.superkiment.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TimeTest {

    private static final String ISO_INSTANT_REGEX =
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z";

    @Test
    void getNowTimestampFormatted_matchesIsoInstantPattern() {
        String ts = Time.GetNowTimestampFormatted();
        assertNotNull(ts, "Le timestamp ne doit pas être null");
        assertTrue(ts.matches(ISO_INSTANT_REGEX),
                "Format attendu : 2026-03-06T12:00:00.000Z, obtenu : " + ts);
    }

    @Test
    void transformTimestampFormatted_knownValue_correctOutput() {
        // Timestamp Unix fixe : 2000-01-01T00:00:00.000Z
        long epoch2000 = 946684800000L;
        String ts = Time.TransformTimestampFormatted(epoch2000);
        assertEquals("2000-01-01T00:00:00Z", ts);
    }

    @Test
    void updateFrameTime_getDeltaFrameTime_isNonNegative() throws InterruptedException {
        Time.UpdateFrameTime();
        Thread.sleep(5);
        Time.UpdateFrameTime();
        assertTrue(Time.GetDeltaFrameTime() >= 0f,
                "Le delta time doit être ≥ 0 après deux updates");
    }

    @Test
    void timer_firesAfterDelay() throws InterruptedException {
        Time t = new Time();
        t.setTimer(30); // 30 ms
        assertFalse(t.updateTimer(), "Le timer ne doit pas encore avoir expiré");
        Thread.sleep(40);
        assertTrue(t.updateTimer(), "Le timer doit avoir expiré après 40ms");
    }

    @Test
    void deltaTime_isPositiveAfterSleep() throws InterruptedException {
        Time t = new Time();
        Thread.sleep(10);
        t.updateDeltaTime();
        assertTrue(t.getDeltaTime() > 0f, "Le deltaTime doit être positif");
    }
}
