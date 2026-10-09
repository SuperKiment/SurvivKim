# SurvivKim

SurvivKim est un jeu multijoueur développé en Java avec une architecture client/serveur modulaire.  
Le projet est structuré en plusieurs sous-modules (client, server, common) et utilise Gradle comme système de build.  
Des outils Web complémentaires sont inclus pour la gestion ou la génération de modèles de formes.

---

## Architecture du projet

```

SurvivKim/
├── client/        # Application cliente (rendu, interface, gameplay côté joueur)
├── server/        # Serveur de jeu (logique centrale, gestion des connexions)
├── common/        # Code partagé entre client et serveur (modèles, protocoles, utilitaires)
├── buildSrc/      # Extensions ou configuration personnalisée de build Gradle
├── WebTools/      # Outils Web (ShapeModel, Node.js)
├── build.gradle
├── settings.gradle
└── gradlew / gradlew.bat

````

### Modules principaux

- **client** : Gestion de l’affichage, interactions utilisateur, communication avec le serveur.
- **server** : Gestion des parties, logique métier, synchronisation des joueurs.
- **common** : Classes partagées (DTO, structures réseau, modèles de données).
- **WebTools/ShapeModel** : Outil Node.js pour la manipulation ou génération de modèles de formes.

---

## Prérequis

- Java (version compatible avec Gradle du projet)
- Gradle (ou utilisation du wrapper inclus)
- Node.js (uniquement pour les outils Web)

---

## Build du projet

Utiliser le wrapper Gradle fourni :

### Sous Linux / macOS

```bash
./gradlew build
````

### Sous Windows

```bash
gradlew.bat build
```

---

## Lancement

### Lancer le serveur

```bash
./gradlew :server:run
```

### Lancer le client

```bash
./gradlew :client:run
```

(Selon la configuration exacte du `build.gradle`, les tâches `run` peuvent être définies par module.)

---

## Outils Web (ShapeModel)

Dans le dossier :

```
WebTools/ShapeModel
```

Installation des dépendances :

```bash
npm install
```

Lancement :

```bash
npm start
```

---

## Description technique

Le projet adopte une architecture réseau classique :

* séparation stricte client / serveur
* code partagé mutualisé dans `common`
* build multi-module Gradle
* outils auxiliaires en JavaScript pour support graphique

---

## Réseau : ajouter du contenu

Tout le réseau partagé vit dans `common/.../net`.

### Ajouter une entité

Une seule classe, dans `common/.../entities` :

```java
public class Zombie extends Entity {
    @Sync public int rage = 1;            // synchronisé automatiquement
    @Sync public Vector2d target = new Vector2d();
    public int localOnly;                 // non annoté : reste local

    public Zombie() { /* shapes, collisions... */ }  // constructeur sans argument requis
}
```

- Faire apparaître : `Main.gameClient.spawn(new Zombie())` côté client, `GameServer.entitiesManager.addEntity(...)` côté serveur.
- Modifier un champ `@Sync` suffit : le serveur diffe chaque tick et propage les changements ; le client fait de même pour son joueur local.
- Position et direction passent par l'UDP, ne pas les annoter.

### Ajouter un packet (hors entités)

1. Un `record` dans `Packets.java` qui implémente `Packet`.
2. Un `router.on(MonPacket.class, (p, ctx) -> ...)` côté récepteur (`Network.ROUTER` serveur, `GameClient.router` client).

Les handlers s'exécutent sur le thread de jeu (file vidée à chaque tick / frame) : pas de concurrence sur l'état du monde.
