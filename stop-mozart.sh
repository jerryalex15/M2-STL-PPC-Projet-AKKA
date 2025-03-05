#!/bin/bash

# Script pour arrêter le système Mozart Game
# Usage: ./stop-mozart.sh

echo "Arrêt du système Mozart Game..."

# Arrêter tous les processus SBT exécutant le jeu
pkill -f 'sbt "run [0-3]"'

echo "Système arrêté."

