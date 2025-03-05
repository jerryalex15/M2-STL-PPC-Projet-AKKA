#!/bin/bash

# Script pour compiler le projet Mozart Game
# Usage: ./build-mozart.sh

echo "Compilation du projet Mozart Game..."

# Nettoyer et compiler le projet
sbt clean compile

if [ $? -eq 0 ]; then
    echo "Compilation réussie!"
    echo "Pour lancer le système, utilisez: ./run-mozart.sh all"
else
    echo "Erreur lors de la compilation."
fi

