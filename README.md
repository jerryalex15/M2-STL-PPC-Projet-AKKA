# Mozart Game - Système Distribué avec Akka

Ce projet implémente le Jeu de Mozart dans un système distribué utilisant Akka.

## Prérequis

- Java 8 ou supérieur
- SBT (Scala Build Tool)
- Un système Linux/Unix (pour les scripts shell)

## Installation

1. Clonez ce dépôt
2. Exécutez le script de configuration:
   ```
   ./setup-mozart.sh
   ```
3. Compilez le projet:
   ```
   ./build-mozart.sh
   ```

## Utilisation

### Lancer le système complet

Pour lancer tous les musiciens (0-3):

```
./run-mozart.sh all
```

### Lancer un musicien spécifique

Pour lancer un musicien avec un ID spécifique (0-3):

```
./run-mozart.sh <id>
```

Par exemple, pour lancer le musicien 0 (conducteur):

```
./run-mozart.sh 0
```

### Arrêter le système

Pour arrêter tous les musiciens:

```
./stop-mozart.sh
```

## Fonctionnement

- Le musicien 0 est le chef d'orchestre initial
- Le chef d'orchestre attend au moins un autre musicien avant de commencer
- Si aucun musicien ne rejoint dans les 30 secondes, le chef d'orchestre quitte
- Si un musicien tombe en panne, le chef d'orchestre évite de lui envoyer des mesures
- Si le chef d'orchestre tombe en panne, un nouveau chef est élu parmi les musiciens restants
- Si le chef d'orchestre reste seul, il attend 30 secondes avant de quitter

## Logs

Les logs de chaque musicien sont enregistrés dans le répertoire `logs/`:

```
logs/musicien-0.log
logs/musicien-1.log
logs/musicien-2.log
logs/musicien-3.log
```

## Architecture du Système

Le système est composé de plusieurs acteurs organisés hiérarchiquement:

### Acteur Principal: Musicien

Chaque instance du système crée un acteur `Musicien` qui représente un musicien dans l'orchestre. Cet acteur:
- Gère la communication avec les autres musiciens
- Détermine s'il est le chef d'orchestre
- Participe à l'élection d'un nouveau chef si nécessaire
- Joue les mesures qui lui sont assignées

### Sous-acteurs

Chaque `Musicien` crée plusieurs sous-acteurs:

1. **DisplayActor**: Affiche les messages dans la console
2. **Checker**: Vérifie l'état des autres musiciens
    - Contient un sous-acteur **Heart** qui envoie des battements de cœur
    - Détecte les musiciens défaillants
3. **Player**: Joue les mesures musicales
4. **Provider**: Fournit des mesures musicales
    - Contient un sous-acteur **Database** qui génère les mesures

## Flux de Messages

Les principaux messages échangés entre les acteurs sont:

- **Start**: Initialise un musicien
- **Alive/Dead**: Indique l'état d'un musicien
- **Beating**: Battement de cœur envoyé régulièrement
- **PlayMeasure/Measure**: Mesures musicales à jouer
- **LearnCurrConductorId**: Informe sur l'identité du chef d'orchestre
- **Election/Elected**: Messages pour l'élection d'un nouveau chef

## Gestion des Défaillances

### Détection des Défaillances

Le système utilise un mécanisme de battement de cœur pour détecter les défaillances:
1. Chaque musicien envoie régulièrement des messages `Beating` via son acteur `Heart`
2. L'acteur `Checker` surveille ces battements
3. Si un musicien ne répond pas pendant 3 cycles (environ 9 secondes), il est considéré comme défaillant

### Défaillance d'un Musicien

Quand un musicien tombe en panne:
1. Le chef d'orchestre reçoit un message `Dead`
2. Le chef d'orchestre retire ce musicien de sa liste de musiciens actifs
3. Le chef d'orchestre évite d'envoyer des mesures à ce musicien

### Défaillance du Chef d'Orchestre

Quand le chef d'orchestre tombe en panne:
1. Les autres musiciens détectent l'absence de battement de cœur
2. Un message `Dead` est généré pour le chef d'orchestre
3. Les musiciens restants lancent une élection avec le message `Election`
4. Le musicien avec l'ID le plus bas devient le nouveau chef d'orchestre
5. Le nouveau chef informe tous les autres musiciens avec `LearnCurrConductorId`

## Tests de Défaillance

Pour tester la résilience du système, vous pouvez simuler différents scénarios de défaillance:

### Test 1: Défaillance d'un Musicien

1. Lancez tous les musiciens: `./run-mozart.sh all`
2. Attendez que le système démarre complètement
3. Arrêtez un musicien (pas le chef d'orchestre): `kill $(pgrep -f "sbt \"run 2\"")`
4. Observez dans les logs que le chef d'orchestre détecte la défaillance et continue avec les musiciens restants

### Test 2: Défaillance du Chef d'Orchestre

1. Lancez tous les musiciens: `./run-mozart.sh all`
2. Attendez que le système démarre complètement
3. Arrêtez le chef d'orchestre: `kill $(pgrep -f "sbt \"run 0\"")`
4. Observez dans les logs qu'un nouveau chef est élu et que la musique continue

### Test 3: Récupération après Défaillance

1. Lancez tous les musiciens: `./run-mozart.sh all`
2. Arrêtez un musicien: `kill $(pgrep -f "sbt \"run 2\"")`
3. Relancez ce musicien: `./run-mozart.sh 2`
4. Observez que le musicien rejoint l'orchestre et participe à nouveau

## Structure du Code

```
src/main/scala/upmc/akka/leader/
├── Projet.scala          # Point d'entrée du programme
├── Musicien.scala        # Acteur principal représentant un musicien
├── DisplayActor.scala    # Acteur pour l'affichage des messages
├── Checker.scala         # Acteur pour vérifier l'état des musiciens
├── Heart.scala           # Acteur pour envoyer des battements de cœur
├── Player.scala          # Acteur pour jouer les mesures
├── Provider.scala        # Acteur pour fournir des mesures
└── Database.scala        # Acteur pour générer des mesures aléatoires
```

## Dépannage

### Problèmes de Port

Si vous rencontrez des erreurs de liaison de port:

```
Binding to /127.0.0.1:2550 failed
```

Assurez-vous qu'aucune autre instance du programme n'utilise déjà ce port. Vous pouvez vérifier avec:

```
netstat -tulpn | grep 255
```

Et tuer les processus existants:

```
kill $(lsof -t -i:2550)
```

### Problèmes de Communication

Si les musiciens ne se détectent pas mutuellement:

1. Vérifiez que tous les musiciens utilisent la même configuration réseau
2. Assurez-vous que les pare-feu ne bloquent pas la communication
3. Vérifiez les logs pour des erreurs de connexion

### Redémarrage Complet

Si le système est dans un état incohérent, effectuez un redémarrage complet:

```
./stop-mozart.sh
pkill -f sbt
./run-mozart.sh all
```

## Personnalisation

### Modifier le Nombre de Musiciens

Pour modifier le nombre de musiciens, vous devez:

1. Ajouter/modifier les configurations dans `application.conf`
2. Mettre à jour les vérifications dans `Projet.scala`
3. Adapter les scripts de lancement

### Modifier le Comportement Musical

Pour personnaliser les mesures musicales:

1. Modifiez la classe `Database.scala` pour générer des mesures différentes
2. Ajustez la méthode `generateMeasure()` selon vos besoins
