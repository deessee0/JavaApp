@echo off
REM Script per generare diagrammi PlantUML usando Docker
REM Usage: scripts\generate-diagrams.bat

REM Directory base del progetto
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set DOCS_DIR=%PROJECT_DIR%\docs
set OUTPUT_DIR=%DOCS_DIR%\images

echo [92m Avvio generazione diagrammi PlantUML...[0m

REM Verifica Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo [91mErrore: Docker non trovato. Installalo per continuare.[0m
    exit /b 1
)

REM Crea directory output se non esiste
if not exist "%OUTPUT_DIR%" (
    echo Creazione directory output: %OUTPUT_DIR%
    mkdir "%OUTPUT_DIR%"
)

REM Esegui PlantUML via Docker
REM Mounta la cartella docs in /data nel container
REM -o images: specifica output relativo a /data (quindi docs/images)
echo Rendering diagrammi in corso...
docker run --rm -e PLANTUML_LIMIT_SIZE=16384 -v "%DOCS_DIR%":/data plantuml/plantuml -tjpg -o images "*.puml"

if %errorlevel% equ 0 (
    echo [92mGenerazione completata![0m
    echo Immagini salvate in: %OUTPUT_DIR%
    dir /b "%OUTPUT_DIR%"
) else (
    echo [91mErrore durante la generazione.[0m
    exit /b 1
)
