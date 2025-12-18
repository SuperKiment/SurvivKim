package com.superkiment.common.utils;

public class StringUtils {
    /**
     * Permet la récupération du dernier terme d'un nom de classe : "com.superkiment.common.entities.EntitiesManager" → "EntitiesManager"
     * @param str le nom de classe
     * @return le dernier terme du nom de classe en String.
     */
    public static String GetLastTerm(String str) {
        String[] parts = str.split("\\.");
        return parts[parts.length - 1];
    }
}
