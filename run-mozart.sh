#!/bin/bash

# Script pour lancer le système Mozart Game
# Usage:
#   ./run-mozart.sh all     - Lance tous les musiciens (0-3)
#   ./run-mozart.sh <num>   - Lance le musicien avec l'ID spécifié (0-3)

# Vérifier que SBT est installé
if ! command -v sbt &> /dev/null; then
    echo "SBT n'est pas installé. Veuillez installer SBT pour exécuter ce projet."
    exit 1
fi

# Fonction pour lancer un musicien
launch_musician() {
    local id=$1
    echo "Lancement du Musicien $id..."

    # Créer un fichier de log pour ce musicien
    mkdir -p logs

    # Lancer le musicien dans un nouveau terminal si possible
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal -- bash -c "sbt \"run $id\" | tee logs/musicien-$id.log; read -p 'Appuyez sur Entrée pour fermer...'"
    elif command -v xterm &> /dev/null; then
        xterm -e "sbt \"run $id\" | tee logs/musicien-$id.log; read -p 'Appuyez sur Entrée pour fermer...'" &
    elif command -v konsole &> /dev/null; then
        konsole -e "sbt \"run $id\" | tee logs/musicien-$id.log; read -p 'Appuyez sur Entrée pour fermer...'" &
    else
        # Si aucun terminal graphique n'est disponible, lancer en arrière-plan
        sbt "run $id" > logs/musicien-$id.log 2>&1 &
        echo "Musicien $id lancé en arrière-plan. Consultez logs/musicien-$id.log pour les détails."
    fi

    # Attendre un peu pour éviter les conflits de ports
    sleep 2
}

# Vérifier les arguments
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 all | <num>"
    echo "  all   - Lance tous les musiciens (0-3)"
    echo "  <num> - Lance le musicien avec l'ID spécifié (0-3)"
    exit 1
fi

# Traiter l'argument
if [ "$1" = "all" ]; then
    echo "Lancement de tous les musiciens..."

    # Lancer les musiciens dans l'ordre (le conducteur en premier)
    for id in 0 1 2 3; do
        launch_musician $id
    done

    echo "Tous les musiciens ont été lancés."
    echo "Pour arrêter le système, utilisez: pkill -f 'sbt run'"

elif [[ "$1" =~ ^[0-3]$ ]]; then
    # Lancer un seul musicien
    launch_musician $1

    echo "Musicien $1 lancé."
    echo "Pour l'arrêter, utilisez: pkill -f 'sbt \"run $1\"'"

else
    echo "Erreur: L'argument doit être 'all' ou un nombre entre 0 et 3."
    exit 1
fi

