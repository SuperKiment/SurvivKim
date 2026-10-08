package com.superkiment.common.net;

import java.util.Map;

/**
 * Tous les packets TCP du jeu. Ajouter un packet = ajouter un record ici + un {@code router.on(...)} côté récepteur.
 * <p>
 * Les entités n'ont PAS besoin de packet dédié : {@link SpawnEntity} / {@link UpdateEntity} transportent
 * n'importe quelle sous-classe d'Entity grâce à {@link EntitySync} et aux champs {@link Sync}.
 */
public final class Packets {
    private Packets() {
    }

    public record Join(String playerId, String playerName) implements Packet {
    }

    public record Heartbeat(long timestamp) implements Packet {
    }

    /**
     * @param type  nom complet de la classe de l'entité
     * @param state valeurs des champs {@link Sync}
     */
    public record SpawnEntity(String type, String id,
                              double x, double y, double dirX, double dirY,
                              Map<String, Object> state) implements Packet {
    }

    /**
     * @param changes uniquement les champs {@link Sync} modifiés
     */
    public record UpdateEntity(String id, Map<String, Object> changes) implements Packet {
    }

    public record RemoveEntity(String id) implements Packet {
    }

    public record CreateBlock(int x, int y) implements Packet {
    }
}
