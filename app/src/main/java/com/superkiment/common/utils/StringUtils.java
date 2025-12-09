package com.superkiment.common.utils;

public class StringUtils {
    public static String GetLastTerm(String str) {
        String[] parts = str.split("\\.");
        return parts[parts.length - 1];
    }
}
