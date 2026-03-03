package com.superkiment.common;

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
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    private static final String BG_BLACK = "\u001B[40m";
    private static final String BG_RED = "\u001B[41m";
    private static final String BG_GREEN = "\u001B[42m";
    private static final String BG_YELLOW = "\u001B[43m";
    private static final String BG_BLUE = "\u001B[44m";
    private static final String BG_PURPLE = "\u001B[45m";
    private static final String BG_CYAN = "\u001B[46m";
    private static final String BG_WHITE = "\u001B[47m";

    public static void log(LogLevel level, String msg) {
        log(level, msg, null);
    }

    public static void log(LogLevel level, String msg, Exception e) {
        String threadName = Thread.currentThread().getName();
        long threadID = Thread.currentThread().threadId();

        System.out.println(
                styleFor(level) + "["
                        + level
                        + "|ts:" + Time.GetNowTimestampFormatted()
                        + "|thread:\"" + threadName + "\" n°" + threadID
                        + "]" + RESET
                        + " " + msg

        );
        if (e != null) System.out.println("Exception : " + e);
    }

    private static String styleFor(LogLevel level) {
        return switch (level) {
            case TRACE -> "\u001B[46m\u001B[30m"; // fond cyan, texte noir
            case DEBUG -> "\u001B[44m\u001B[37m"; // fond bleu, texte blanc
            case INFO -> "\u001B[42m\u001B[30m"; // fond vert, texte noir
            case WARN -> "\u001B[43m\u001B[30m"; // fond jaune, texte noir
            case ERROR -> "\u001B[41m\u001B[37m"; // fond rouge, texte blanc
        };
    }
}
