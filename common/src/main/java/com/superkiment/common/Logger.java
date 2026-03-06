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

    /**
     * Enregistre un message de log avec le niveau spécifié.
     *
     * <p>Format de sortie :
     * <pre>
     * [LEVEL|ts:timestamp|thread:"name" n°id] message
     * </pre>
     *
     * <p>Inclut automatiquement :
     * <ul>
     *     <li>Le niveau de log</li>
     *     <li>Un timestamp formaté</li>
     *     <li>Le nom du thread courant</li>
     *     <li>L'identifiant du thread courant</li>
     * </ul>
     *
     * @param level niveau de sévérité du log
     * @param msg   message à afficher
     */
    public static void log(LogLevel level, String msg) {
        log(level, msg, null);
    }

    /**
     * Enregistre un message de log avec le niveau spécifié et une exception optionnelle.
     *
     * <p>Le message est affiché avec métadonnées :
     * niveau, timestamp, nom du thread et identifiant du thread.
     *
     * <p>Si une exception est fournie, elle est affichée après le message.
     * (Implémentation actuelle : affiche uniquement {@code e.toString()},
     * le stacktrace complet devra être ajouté ultérieurement.)
     *
     * @param level niveau de sévérité du log
     * @param msg   message à afficher
     * @param e     exception associée au log, peut être {@code null}
     */
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
        //TODO: Changer le print des erreurs pour voir le stacktrace
        if (e != null) System.out.println("Exception : " + e);
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
