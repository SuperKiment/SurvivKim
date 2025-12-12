package com.superkiment.client.graphics;

import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Classe utilitaire pour dessiner des formes géométriques simples
 */
public class ReusableShape {

    private final Vector2f position;
    private final Vector3f color;
    private float rotation; // en degrés

    public ReusableShape(float x, float y) {
        this.position = new Vector2f(x, y);
        this.color = new Vector3f(1.0f, 1.0f, 1.0f); // Blanc par défaut
        this.rotation = 0.0f;
    }

    /**
     * Définir la couleur (RGB entre 0 et 1)
     */
    public ReusableShape setColor(float r, float g, float b) {
        this.color.set(r, g, b);
        return this;
    }

    /**
     * Définir la rotation en degrés
     */
    public ReusableShape setRotation(float degrees) {
        this.rotation = degrees;
        return this;
    }

    /**
     * Définir la position
     */
    public ReusableShape setPosition(float x, float y) {
        this.position.set(x, y);
        return this;
    }

    /**
     * Dessiner un rectangle
     * @param width largeur
     * @param height hauteur
     */
    public void drawRect(float width, float height) {
        glPushMatrix();
        glTranslatef(position.x, position.y, 0);
        glRotatef(rotation, 0, 0, 1);
        glColor3f(color.x, color.y, color.z);

        glBegin(GL_QUADS);
        glVertex2f(-width / 2, -height / 2);
        glVertex2f(width / 2, -height / 2);
        glVertex2f(width / 2, height / 2);
        glVertex2f(-width / 2, height / 2);
        glEnd();

        glPopMatrix();
    }

    /**
     * Dessiner un rectangle avec contour
     */
    public void drawRectOutline(float width, float height, float lineWidth) {
        // Remplissage
        drawRect(width, height);

        // Contour noir
        glPushMatrix();
        glTranslatef(position.x, position.y, 0);
        glRotatef(rotation, 0, 0, 1);
        glColor3f(0, 0, 0);
        glLineWidth(lineWidth);

        glBegin(GL_LINE_LOOP);
        glVertex2f(-width / 2, -height / 2);
        glVertex2f(width / 2, -height / 2);
        glVertex2f(width / 2, height / 2);
        glVertex2f(-width / 2, height / 2);
        glEnd();

        glPopMatrix();
    }

    /**
     * Dessiner un cercle
     * @param radius rayon
     * @param segments nombre de segments (plus = plus lisse)
     */
    public void drawCircle(float radius, int segments) {
        glPushMatrix();
        glTranslatef(position.x, position.y, 0);
        glColor3f(color.x, color.y, color.z);

        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(0, 0); // Centre

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            glVertex2f(x, y);
        }
        glEnd();

        glPopMatrix();
    }

    /**
     * Dessiner un cercle avec contour
     */
    public void drawCircleOutline(float radius, int segments, float lineWidth) {
        // Remplissage
        drawCircle(radius, segments);

        // Contour
        glPushMatrix();
        glTranslatef(position.x, position.y, 0);
        glColor3f(0, 0, 0);
        glLineWidth(lineWidth);

        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            float x = (float) Math.cos(angle) * radius;
            float y = (float) Math.sin(angle) * radius;
            glVertex2f(x, y);
        }
        glEnd();

        glPopMatrix();
    }

    /**
     * Dessiner un triangle
     * @param size taille du triangle
     */
    public void drawTriangle(float size) {
        glPushMatrix();
        glTranslatef(position.x, position.y, 0);
        glRotatef(rotation, 0, 0, 1);
        glColor3f(color.x, color.y, color.z);

        float height = (float) (size * Math.sqrt(3) / 2);

        glBegin(GL_TRIANGLES);
        glVertex2f(0, height / 2);              // Sommet
        glVertex2f(-size / 2, -height / 2);     // Bas gauche
        glVertex2f(size / 2, -height / 2);      // Bas droit
        glEnd();

        glPopMatrix();
    }

    /**
     * Dessiner un triangle avec contour
     */
    public void drawTriangleOutline(float size, float lineWidth) {
        // Remplissage
        drawTriangle(size);

        // Contour
        glPushMatrix();
        glTranslatef(position.x, position.y, 0);
        glRotatef(rotation, 0, 0, 1);
        glColor3f(0, 0, 0);
        glLineWidth(lineWidth);

        float height = (float) (size * Math.sqrt(3) / 2);

        glBegin(GL_LINE_LOOP);
        glVertex2f(0, height / 2);
        glVertex2f(-size / 2, -height / 2);
        glVertex2f(size / 2, -height / 2);
        glEnd();

        glPopMatrix();
    }

    /**
     * Dessiner un triangle personnalisé avec 3 points
     */
    public void drawTriangleCustom(float x1, float y1, float x2, float y2, float x3, float y3) {
        glPushMatrix();
        glTranslatef(position.x, position.y, 0);
        glRotatef(rotation, 0, 0, 1);
        glColor3f(color.x, color.y, color.z);

        glBegin(GL_TRIANGLES);
        glVertex2f(x1, y1);
        glVertex2f(x2, y2);
        glVertex2f(x3, y3);
        glEnd();

        glPopMatrix();
    }

    /**
     * Dessiner une ligne
     */
    public void drawLine(float x2, float y2, float lineWidth) {
        glPushMatrix();
        glColor3f(color.x, color.y, color.z);
        glLineWidth(lineWidth);

        glBegin(GL_LINES);
        glVertex2f(position.x, position.y);
        glVertex2f(x2, y2);
        glEnd();

        glPopMatrix();
    }

    // Getters
    public Vector2f getPosition() {
        return position;
    }

    public Vector3f getColor() {
        return color;
    }

    public float getRotation() {
        return rotation;
    }
}