package com.superkiment.common;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String WHITE = "\u001B[37m";
    private static final String BG_RED = "\u001B[41m";
    private static final String BG_GREEN = "\u001B[42m";
    private static final String BG_YELLOW = "\u001B[43m";
    private static final String BG_BLUE = "\u001B[44m";
    private static final String BG_CYAN = "\u001B[46m";

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILENAME = "app-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".log";
    private static final Path LOG_PATH = Path.of(LOG_DIR, LOG_FILENAME);

    private static LogLevel minimumLevel = LogLevel.TRACE;
    private static PrintWriter fileWriter;

    static {
        try {
            Files.createDirectories(Path.of(LOG_DIR));
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOG_PATH.toFile(), true)));

            // Fermeture propre à l'arrêt de la JVM
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (fileWriter != null) {
                    fileWriter.flush();
                    fileWriter.close();
                }
            }));
        } catch (IOException ex) {
            System.err.println("[Logger] Impossible de créer le fichier de log : " + ex.getMessage());
        }
    }

    /**
     * Définit le niveau minimum à partir duquel les logs sont émis.
     */
    public static void setMinimumLevel(LogLevel level) {
        minimumLevel = level;
    }

    public static void log(LogLevel level, String msg) {
        log(level, msg, null);
    }

    public static void log(LogLevel level, String msg, Exception e) {
        if (level.ordinal() < minimumLevel.ordinal()) return;

        String threadName = Thread.currentThread().getName();
        long threadID = Thread.currentThread().threadId();
        String timestamp = Time.GetNowTimestampFormatted();

        String header = "[" + level + "|ts:" + timestamp
                + "|thread:\"" + threadName + "\" n°" + threadID + "]";
        System.out.println(styleFor(level) + header + RESET + " " + msg);
        if (e != null) {
            System.out.println(RED + "Exception : " + e + RESET);
            printStackTrace(e, System.out::println);
        }

        if (fileWriter != null) {
            synchronized (fileWriter) {
                fileWriter.println(header + " " + msg);
                if (e != null) {
                    fileWriter.println("Exception : " + e);
                    printStackTrace(e, fileWriter::println);
                }
                fileWriter.flush(); // garantit l'écriture même en cas de crash
            }
        }
    }

    public static void trace(String msg) {
        log(LogLevel.TRACE, msg);
    }

    public static void debug(String msg) {
        log(LogLevel.DEBUG, msg);
    }

    public static void info(String msg) {
        log(LogLevel.INFO, msg);
    }

    public static void warn(String msg) {
        log(LogLevel.WARN, msg);
    }

    public static void error(String msg) {
        log(LogLevel.ERROR, msg);
    }

    public static void error(String msg, Exception e) {
        log(LogLevel.ERROR, msg, e);
    }

    private static void printStackTrace(Exception e, java.util.function.Consumer<String> sink) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        for (String line : sw.toString().split(System.lineSeparator())) {
            sink.accept("    " + line);
        }
    }

    private static String styleFor(LogLevel level) {
        return switch (level) {
            case TRACE -> BG_CYAN + BLACK;
            case DEBUG -> BG_BLUE + BLACK;
            case INFO -> BG_GREEN + BLACK;
            case WARN -> BG_YELLOW + BLACK;
            case ERROR -> BG_RED + WHITE;
        };
    }
}