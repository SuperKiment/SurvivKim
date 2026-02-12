package com.superkiment.client.graphics;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire centralisé pour les polices du jeu
 */
public class FontManager {
    private Map<String, BitmapFont> fonts = new HashMap<>();
    private BitmapFont defaultFont;

    /**
     * Initialise le gestionnaire avec les polices du jeu
     */
    public void initialize() {
        defaultFont = new BitmapFont("Arial", 32, true);
        fonts.put("default", defaultFont);

        // Charger depuis un fichier externe
        loadCustomFontFromResources("minecraft", "/assets/fonts/Minecraft.ttf", 48);
    }

    /**
     * Charge une police custom depuis un fichier (pour les fichiers externes)
     */
    public void loadCustomFont(String name, String fontPath, int fontSize) {
        try {
            BitmapFont font = new BitmapFont(fontPath, fontSize, true, true);
            fonts.put(name, font);
            System.out.println("Police '" + name + "' chargée avec succès");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la police: " + fontPath);
            e.printStackTrace();
        }
    }

    /**
     * Charge une police custom depuis les resources (dans le JAR)
     */
    public void loadCustomFontFromResources(String name, String resourcePath, int fontSize) {
        try {
            // IMPORTANT: le chemin doit commencer par "/" pour les resources
            InputStream fontStream = getClass().getResourceAsStream(resourcePath);
            if (fontStream == null) {
                throw new RuntimeException("Police introuvable dans les resources: " + resourcePath);
            }
            BitmapFont font = new BitmapFont(fontStream, fontSize, true);
            fonts.put(name, font);
            System.out.println("Police '" + name + "' chargée depuis les resources avec succès");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la police: " + resourcePath);
            e.printStackTrace();
        }
    }

    /**
     * Récupère une police par son nom
     */
    public BitmapFont getFont(String name) {
        BitmapFont font = fonts.get(name);
        return font != null ? font : defaultFont;
    }

    /**
     * Récupère la police par défaut
     */
    public BitmapFont getDefaultFont() {
        return defaultFont;
    }

    /**
     * Nettoie toutes les polices
     */
    public void cleanup() {
        for (BitmapFont font : fonts.values()) {
            font.cleanup();
        }
        fonts.clear();
    }
}