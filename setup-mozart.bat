@echo off
echo Configuration de l'environnement Mozart Game...

:: Créer le répertoire de logs
if not exist logs mkdir logs

echo Configuration terminée.
echo Pour compiler le projet, utilisez: build-mozart.bat
echo Pour lancer le système, utilisez: run-mozart.bat all
echo Pour arrêter le système, utilisez: stop-mozart.bat
pause

