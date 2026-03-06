package com.superkiment.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour Logger.
 * On capture System.out pour vérifier que les bons messages apparaissent.
 */
public class LoggerTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
        // Remettre le niveau minimum au plus bas pour chaque test
        Logger.setMinimumLevel(Logger.LogLevel.TRACE);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        Logger.setMinimumLevel(Logger.LogLevel.TRACE);
    }

    // ─────────────────────────── Niveaux de log ───────────────────────────

    @Test
    void trace_shouldPrintMessage() {
        Logger.trace("message trace");
        String out = capturedOut.toString();
        assertTrue(out.contains("message trace"), "TRACE doit apparaître quand le niveau est TRACE");
        assertTrue(out.contains("TRACE"), "L'en-tête doit contenir le niveau");
    }

    @Test
    void debug_shouldPrintMessage() {
        Logger.debug("message debug");
        assertTrue(capturedOut.toString().contains("message debug"));
    }

    @Test
    void info_shouldPrintMessage() {
        Logger.info("message info");
        assertTrue(capturedOut.toString().contains("message info"));
    }

    @Test
    void warn_shouldPrintMessage() {
        Logger.warn("message warn");
        assertTrue(capturedOut.toString().contains("message warn"));
    }

    @Test
    void error_shouldPrintMessage() {
        Logger.error("message error");
        assertTrue(capturedOut.toString().contains("message error"));
    }

    // ──────────────────── Filtrage par niveau minimum ────────────────────

    @Test
    void setMinimumLevel_WARN_shouldSuppressInfoAndBelow() {
        Logger.setMinimumLevel(Logger.LogLevel.WARN);

        Logger.trace("trace-ignoré");
        Logger.debug("debug-ignoré");
        Logger.info("info-ignoré");

        String out = capturedOut.toString();
        assertFalse(out.contains("trace-ignoré"), "TRACE doit être filtré quand niveau >= WARN");
        assertFalse(out.contains("debug-ignoré"), "DEBUG doit être filtré quand niveau >= WARN");
        assertFalse(out.contains("info-ignoré"),  "INFO doit être filtré quand niveau >= WARN");
    }

    @Test
    void setMinimumLevel_WARN_shouldAllowWarnAndError() {
        Logger.setMinimumLevel(Logger.LogLevel.WARN);

        Logger.warn("warn-visible");
        Logger.error("error-visible");

        String out = capturedOut.toString();
        assertTrue(out.contains("warn-visible"),  "WARN doit passer avec niveau WARN");
        assertTrue(out.contains("error-visible"), "ERROR doit passer avec niveau WARN");
    }

    @Test
    void setMinimumLevel_ERROR_shouldSuppressEverythingBelow() {
        Logger.setMinimumLevel(Logger.LogLevel.ERROR);

        Logger.trace("t"); Logger.debug("d"); Logger.info("i"); Logger.warn("w");
        String out = capturedOut.toString();

        assertFalse(out.contains("t") || out.contains("d") || out.contains("i") || out.contains("w"),
                "Seul ERROR doit passer");
    }

    // ───────────────────── Log avec exception ─────────────────────────────

    @Test
    void error_withException_shouldPrintExceptionInfo() {
        Exception ex = new IllegalStateException("problème test");
        Logger.error("erreur avec exception", ex);
        String out = capturedOut.toString();

        assertTrue(out.contains("erreur avec exception"));
        assertTrue(out.contains("IllegalStateException") || out.contains("problème test"),
                "La stack trace ou le message de l'exception doit apparaître");
    }

    // ───────────────────── En-tête structuré ──────────────────────────────

    @Test
    void log_headerContainsThreadAndTimestamp() {
        Logger.info("structure test");
        String out = capturedOut.toString();

        assertTrue(out.contains("thread:"), "L'en-tête doit contenir le nom du thread");
        assertTrue(out.contains("ts:"),     "L'en-tête doit contenir un timestamp");
    }
}
