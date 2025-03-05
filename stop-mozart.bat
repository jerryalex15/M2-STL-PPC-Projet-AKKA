@echo off
echo Arrêt du système Mozart Game...

:: Arrêter tous les processus SBT exécutant le jeu
taskkill /f /fi "WINDOWTITLE eq Musicien*" >nul 2>nul
taskkill /f /fi "IMAGENAME eq java.exe" /fi "WINDOWTITLE eq sbt*" >nul 2>nul

echo Système arrêté.
pause

