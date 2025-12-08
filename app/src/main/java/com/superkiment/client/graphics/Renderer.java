package com.superkiment.client.graphics;

import com.superkiment.common.Entity;

import java.util.Map;

public class Renderer {
    public Renderer() {
    }

    public void renderEntities(Map<String, Entity> entities, Entity localPlayer) {
        // Dessiner toutes les entités
        for (Entity entity : entities.values()) {
            boolean isLocal = entity.id.equals(localPlayer.id);

            // Joueur local = rouge, autres joueurs = bleu
            if (isLocal) {
                new Shape((float) entity.pos.x, (float) entity.pos.y).setColor(1.0f, 0.3f, 0.3f).drawRect(40, 40);
            } else {
                new Shape((float) entity.pos.x, (float) entity.pos.y).setColor(0.3f, 0.5f, 1.0f).drawRect(40, 40);
            }

            // Afficher le nom (simplifié - tu peux utiliser une vraie lib de texte)
            // Pour l'instant, juste un petit cercle au-dessus
            new Shape(
                    (float) entity.pos.x,
                    (float) entity.pos.y - 30)
                    .setColor(1.0f, 1.0f, 1.0f)
                    .drawCircle(3, 8);
        }
    }

    public void RenderFloor() {

    }
}
