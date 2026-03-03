package com.superkiment.common;

public class Logger {
    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public static void log(LogLevel level, String msg) {
        log(level, msg, null);
    }

    public static void log(LogLevel level, String msg, Exception e) {
        String threadName = Thread.currentThread().getName();
        long threadID = Thread.currentThread().threadId();
        long timestamp = System.currentTimeMillis();

        System.out.println(
                "["
                        + level.toString()
                        + "|ts:" + timestamp
                        + "|thread:\"" + threadName + "\"-ID:" + threadID
                        + "] " + msg
        );
        if (e != null) System.out.println("Exception : " + e);
    }
}
