# 🎵 Mozart Game - Système Distribué avec Akka

Projet réalisé dans le cadre du **TME 9 – Systèmes Distribués**  
👨‍🏫 Encadrant : agonc@ircam.fr

## 👥 Auteurs

- Nandraina RAZAFINDRAIBE – jerryalex15  
- Florian CODEBECQ – CFlorian04

---

## 📜 Description

Ce projet implémente une version distribuée du **Jeu de Mozart** à l'aide du framework **AKKA** (Java ou Scala).  
Des **musiciens** (acteurs) sont exécutés en parallèle, et l’un d’eux prend le rôle de **chef d’orchestre**.  
Le système est **résilient aux pannes** : si un musicien ou le chef d’orchestre tombe, le jeu continue avec les acteurs restants.

---

## 🧱 Architecture

- Chaque **musicien** est un acteur AKKA avec éventuellement des sous-acteurs.
- Le **chef d’orchestre** :
  - Lance les dés et choisit une mesure.
  - Envoie les mesures aux musiciens actifs.
  - Ignore les musiciens en panne.
  - Peut être remplacé en cas de panne.
- Les musiciens restants élisent un nouveau chef si le précédent est défaillant.
- Délai de **30 secondes** pour démarrer ou arrêter la session si nécessaire.

---

## 🚀 Lancer le projet

- Java 8 ou supérieur
- SBT (Scala Build Tool)
- Un système Linux/Unix (pour les scripts shell)
- 
### ▶️ Exécution

```bash
sbt run
