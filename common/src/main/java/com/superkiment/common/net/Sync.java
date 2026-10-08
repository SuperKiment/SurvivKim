package com.superkiment.common.net;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Champ d'entité synchronisé automatiquement entre client et serveur.
 * <p>
 * Toute modification est détectée par diff et propagée ; aucun code réseau à écrire.
 * Types supportés : primitifs, String, enums, Vector2d, tableaux de ceux-ci, ou tout Serializable immuable.
 * La position et la direction passent par l'UDP et ne doivent pas être annotées.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sync {
}
