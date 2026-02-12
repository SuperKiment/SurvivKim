package com.superkiment.client.graphics;

import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * Classe pour gérer le rendu de texte avec une bitmap font
 */
public class BitmapFont {
    private int textureId;
    private int textureWidth = 512;
    private int textureHeight = 512;
    private Map<Character, CharInfo> charMap = new HashMap<>();
    private int baseFontSize; // Taille de base de la police

    private static class CharInfo {
        float u, v;           // Coordonnées UV de départ
        float u2, v2;         // Coordonnées UV de fin
        int width, height;    // Dimensions du caractère
        int xOffset, yOffset; // Offset pour le positionnement
    }

    /**
     * Crée une bitmap font à partir d'une police système
     * @param fontName Nom de la police (ex: "Arial", "Verdana")
     * @param fontSize Taille de la police
     * @param antialiasing Activer l'antialiasing
     */
    public BitmapFont(String fontName, int fontSize, boolean antialiasing) {
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        this.baseFontSize = fontSize;
        generateTexture(font, antialiasing);
    }

    /**
     * Crée une bitmap font à partir d'un fichier de police custom
     * @param fontPath Chemin vers le fichier .ttf ou .otf
     * @param fontSize Taille de la police
     * @param antialiasing Activer l'antialiasing
     */
    public BitmapFont(String fontPath, int fontSize, boolean antialiasing, boolean isCustomFont) {
        try {
            Font font = loadCustomFont(fontPath, fontSize);
            this.baseFontSize = fontSize;
            generateTexture(font, antialiasing);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement de la police custom: " + fontPath, e);
        }
    }

    /**
     * Crée une bitmap font à partir d'un InputStream (pour les polices dans les resources)
     * @param fontStream InputStream du fichier de police
     * @param fontSize Taille de la police
     * @param antialiasing Activer l'antialiasing
     */
    public BitmapFont(InputStream fontStream, int fontSize, boolean antialiasing) {
        try {
            Font font = loadCustomFontFromStream(fontStream, fontSize);
            this.baseFontSize = fontSize;
            generateTexture(font, antialiasing);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement de la police custom depuis le stream", e);
        }
    }

    /**
     * Charge une police custom depuis un fichier
     */
    private Font loadCustomFont(String fontPath, int fontSize) throws Exception {
        File fontFile = new File(fontPath);
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
        return font.deriveFont(Font.PLAIN, fontSize);
    }

    /**
     * Charge une police custom depuis un InputStream
     */
    private Font loadCustomFontFromStream(InputStream fontStream, int fontSize) throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
        return font.deriveFont(Font.PLAIN, fontSize);
    }

    /**
     * Génère la texture de la police
     */
    private void generateTexture(Font font, boolean antialiasing) {
        // Créer une image temporaire pour dessiner les caractères
        BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Configuration du rendu
        if (antialiasing) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        g.setFont(font);
        g.setColor(Color.WHITE);

        FontMetrics metrics = g.getFontMetrics();

        int x = 0;
        int y = metrics.getAscent();
        int rowHeight = 0;

        // Caractères à inclure (ASCII de base + quelques accents)
        String chars = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~àâäéèêëïîôùûüÿçÀÂÄÉÈÊËÏÎÔÙÛÜŸÇ€";

        for (char c : chars.toCharArray()) {
            int charWidth = metrics.charWidth(c);
            int charHeight = metrics.getHeight();

            // Passer à la ligne suivante si nécessaire
            if (x + charWidth >= textureWidth) {
                x = 0;
                y += rowHeight + 2;
                rowHeight = 0;
            }

            // Vérifier qu'on ne dépasse pas la hauteur
            if (y + charHeight >= textureHeight) {
                System.err.println("Warning: Texture trop petite pour tous les caractères");
                break;
            }

            // Dessiner le caractère
            g.drawString(String.valueOf(c), x, y);

            // Sauvegarder les informations du caractère
            CharInfo info = new CharInfo();
            info.width = charWidth;
            info.height = charHeight;
            info.xOffset = 0;
            info.yOffset = -metrics.getAscent();

            // Coordonnées UV (normalisées entre 0 et 1)
            info.u = (float) x / textureWidth;
            info.v = (float) (y - metrics.getAscent()) / textureHeight;
            info.u2 = (float) (x + charWidth) / textureWidth;
            info.v2 = (float) (y - metrics.getAscent() + charHeight) / textureHeight;

            charMap.put(c, info);

            x += charWidth + 2;
            rowHeight = Math.max(rowHeight, charHeight);
        }

        g.dispose();

        // Convertir l'image en ByteBuffer pour OpenGL
        ByteBuffer buffer = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);

        for (int py = 0; py < textureHeight; py++) {
            for (int px = 0; px < textureWidth; px++) {
                int pixel = img.getRGB(px, py);

                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Rouge
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Vert
                buffer.put((byte) (pixel & 0xFF));         // Bleu
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }

        buffer.flip();

        // Créer la texture OpenGL
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight,
                0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    }

    /**
     * Dessine du texte à la position donnée
     */
    public void drawText(String text, float x, float y, float scale, float r, float g, float b) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);

        glColor3f(r, g, b);

        float currentX = x;

        glBegin(GL_QUADS);
        for (char c : text.toCharArray()) {
            CharInfo info = charMap.get(c);

            if (info == null) {
                // Caractère non supporté, utiliser un espace
                info = charMap.get(' ');
                if (info == null) continue;
            }

            float width = info.width * scale;
            float height = info.height * scale;
            float xOff = info.xOffset * scale;
            float yOff = info.yOffset * scale;

            // Coin inférieur gauche
            glTexCoord2f(info.u, info.v2);
            glVertex2f(currentX + xOff, y + yOff + height);

            // Coin inférieur droit
            glTexCoord2f(info.u2, info.v2);
            glVertex2f(currentX + xOff + width, y + yOff + height);

            // Coin supérieur droit
            glTexCoord2f(info.u2, info.v);
            glVertex2f(currentX + xOff + width, y + yOff);

            // Coin supérieur gauche
            glTexCoord2f(info.u, info.v);
            glVertex2f(currentX + xOff, y + yOff);

            currentX += width;
        }
        glEnd();

        glDisable(GL_TEXTURE_2D);
    }

    /**
     * Calcule la largeur d'un texte
     */
    public float getTextWidth(String text, float scale) {
        float width = 0;
        for (char c : text.toCharArray()) {
            CharInfo info = charMap.get(c);
            if (info != null) {
                width += info.width * scale;
            }
        }
        return width;
    }

    /**
     * Retourne la taille de base de la police
     */
    public int getBaseFontSize() {
        return baseFontSize;
    }

    /**
     * Nettoie les ressources
     */
    public void cleanup() {
        glDeleteTextures(textureId);
    }
}