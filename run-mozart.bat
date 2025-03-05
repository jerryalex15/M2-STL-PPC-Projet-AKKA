@echo off
setlocal enabledelayedexpansion

:: Script pour lancer le système Mozart Game
:: Usage:
::   run-mozart.bat all     - Lance tous les musiciens (0-3)
::   run-mozart.bat <num>   - Lance le musicien avec l'ID spécifié (0-3)

:: Vérifier que SBT est installé
where sbt >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo SBT n'est pas installé ou n'est pas dans le PATH. Veuillez installer SBT pour exécuter ce projet.
    pause
    exit /b 1
)

:: Fonction pour lancer un musicien
:launch_musician
    set id=%~1
    echo Lancement du Musicien %id%...

    :: Créer un répertoire de log si nécessaire
    if not exist logs mkdir logs

    :: Lancer le musicien dans une nouvelle fenêtre CMD
    :: Utiliser une redirection simple vers un fichier au lieu de tee
    start cmd /k "title Musicien %id% && sbt \"run %id%\" > logs\musicien-%id%.log 2>&1"

    :: Attendre un peu pour éviter les conflits de ports
    timeout /t 2 /nobreak >nul
    goto :eof

:: Vérifier les arguments
if "%~1"=="" (
    echo Usage: %0 all ^| ^<num^>
    echo   all   - Lance tous les musiciens (0-3)
    echo   ^<num^> - Lance le musicien avec l'ID spécifié (0-3)
    pause
    exit /b 1
)

:: Traiter l'argument
if "%~1"=="all" (
    echo Lancement de tous les musiciens...

    :: Lancer les musiciens dans l'ordre (le conducteur en premier)
    call :launch_musician 0
    call :launch_musician 1
    call :launch_musician 2
    call :launch_musician 3

    echo Tous les musiciens ont été lancés.
    echo Pour arrêter le système, utilisez: stop-mozart.bat

) else (
    :: Vérifier si l'argument est un chiffre entre 0 et 3
    set "arg=%~1"
    if "!arg!" == "0" (
        call :launch_musician 0
    ) else if "!arg!" == "1" (
        call :launch_musician 1
    ) else if "!arg!" == "2" (
        call :launch_musician 2
    ) else if "!arg!" == "3" (
        call :launch_musician 3
    ) else (
        echo Erreur: L'argument doit être 'all' ou un nombre entre 0 et 3.
        pause
        exit /b 1
    )

    echo Musicien %~1 lancé.
    echo Pour l'arrêter, utilisez: stop-mozart.bat
)

pause

