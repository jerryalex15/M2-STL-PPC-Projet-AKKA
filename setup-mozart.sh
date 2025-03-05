#!/bin/bash

# Script pour configurer l'environnement Mozart Game
# Usage: ./setup-mozart.sh

echo "Configuration de l'environnement Mozart Game..."

# Rendre les scripts exécutables
chmod +x run-mozart.sh stop-mozart.sh build-mozart.sh

# Créer le répertoire de logs
mkdir -p logs

echo "Configuration terminée."
echo "Pour compiler le projet, utilisez: ./build-mozart.sh"
echo "Pour lancer le système, utilisez: ./run-mozart.sh all"
echo "Pour arrêter le système, utilisez: ./stop-mozart.sh"

