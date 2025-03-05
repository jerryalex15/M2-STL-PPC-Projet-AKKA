@echo off
echo Compilation du projet Mozart Game...

:: Nettoyer et compiler le projet
call sbt clean compile

if %ERRORLEVEL% == 0 (
    echo Compilation réussie!
    echo Pour lancer le système, utilisez: run-mozart.bat all
) else (
    echo Erreur lors de la compilation.
)
pause

