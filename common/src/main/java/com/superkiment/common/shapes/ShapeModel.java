package com.superkiment.common.shapes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.superkiment.common.collisions.Collisionable;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Un objet permettant une collection de Shapes et de toutes les render d'un coup, permettant de les bind à une direction.
 */
public class ShapeModel {
    public List<Shape> shapes;

    public ShapeModel() {
        shapes = new CopyOnWriteArrayList<>();
    }

    public void addShape(Shape s) {
        shapes.add(s);
    }

    public void addShapes(List<Shape> newShapes) {
        shapes.addAll(newShapes);
    }

    public void update(Collisionable collisionable) {
    }

    /**
     * Construit un ShapeModel à partir d'un JSON exporté par le ShapeModel Editor.
     * <p>
     * Format attendu :
     * {
     * "shapes": [
     * {
     * "type": "RECT",
     * "position":   { "x": 0,   "y": 0   },
     * "dimensions": { "x": 80,  "y": 50  },
     * "color":      { "r": 1.0, "g": 0.0, "b": 0.0 },
     * "text": {                            // optionnel
     * "content":  "Hello",
     * "fontSize": 32,
     * "color":    { "r": 0, "g": 0, "b": 0 },
     * "offset":   { "x": 0, "y": 0 },
     * "fontName": "minecraft"
     * }
     * }
     * ]
     * }
     *
     * @param json Chaîne JSON brute
     * @return ShapeModel peuplé
     */
    public static ShapeModel fromJson(String json) {
        ShapeModel model = new ShapeModel();
        JsonArray arr = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonArray("shapes");

        for (JsonElement el : arr) {
            JsonObject jo = el.getAsJsonObject();
            JsonObject pos = jo.getAsJsonObject("position");
            JsonObject dim = jo.getAsJsonObject("dimensions");
            JsonObject col = jo.getAsJsonObject("color");

            Shape s = new Shape(
                    new Vector2d(pos.get("x").getAsDouble(), pos.get("y").getAsDouble()),
                    new Vector2d(dim.get("x").getAsDouble(), dim.get("y").getAsDouble()),
                    Shape.ShapeType.valueOf(jo.get("type").getAsString())
            );

            s.setColor(
                    col.get("r").getAsFloat(),
                    col.get("g").getAsFloat(),
                    col.get("b").getAsFloat()
            );

            // Nom optionnel
            if (jo.has("name") && !jo.get("name").isJsonNull()) {
                s.setName(jo.get("name").getAsString());
            }

            // Texte optionnel
            if (jo.has("text")) {
                JsonObject txt = jo.getAsJsonObject("text");
                JsonObject tcol = txt.getAsJsonObject("color");
                JsonObject offset = txt.getAsJsonObject("offset");

                s.setText(
                        txt.get("content").getAsString(),
                        txt.get("fontSize").getAsFloat(),
                        new Vector3d(
                                tcol.get("r").getAsDouble(),
                                tcol.get("g").getAsDouble(),
                                tcol.get("b").getAsDouble()
                        ),
                        txt.get("fontName").getAsString()
                );

                double ox = offset.get("x").getAsDouble();
                double oy = offset.get("y").getAsDouble();
                if (ox != 0 || oy != 0) {
                    s.setTextOffset(ox, oy);
                }
            }

            model.addShape(s);
        }

        return model;
    }

    /**
     * Charge un ShapeModel depuis un fichier JSON sur le classpath.
     * <p>
     * Exemple d'utilisation :
     * ShapeModel sm = ShapeModel.fromJsonFile("/assets/models/player.json");
     *
     * @param resourcePath Chemin absolu depuis la racine du classpath
     * @return ShapeModel peuplé
     * @throws Exception Si le fichier est introuvable ou le JSON invalide
     */
    public static ShapeModel fromJsonFile(String resourcePath) throws Exception {
        try (InputStream is = ShapeModel.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException(
                        "Resource not found on classpath: " + resourcePath);
            }
            return fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public boolean isPointInShape(Vector2d point) {
        for (Shape shape : shapes) {
            if (shape.isPointInShape(point)) return true;
        }
        return false;
    }

    /**
     * Retourne la première shape dont le nom correspond exactement.
     * Utile pour animer ou modifier une shape depuis le code de jeu.
     *
     * @param name Nom défini dans l'éditeur
     * @return La Shape correspondante, ou {@code null} si introuvable
     */
    public Shape getShapeByName(String name) {
        for (Shape s : shapes) {
            if (name.equals(s.name)) return s;
        }
        return null;
    }
}
