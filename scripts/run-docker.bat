@echo off
REM Script di avvio rapido con Docker (Windows)
REM Uso: scripts\run-docker.bat

REM Cambia alla directory root del progetto (una directory sopra scripts/)
cd /d "%~dp0\.."

echo ================================================
echo 🐳 Avvio Padel App con Docker
echo ================================================
echo.

REM ========== VERIFICA PREREQUISITI ==========

echo 📋 Verifica prerequisiti...
echo.

REM Check Docker
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ERRORE: Docker non trovato
    echo    Installa Docker Desktop:
    echo    - Windows: https://www.docker.com/products/docker-desktop
    echo.
    pause
    exit /b 1
)

echo ✅ Docker installato
echo.

REM Check se Docker è in esecuzione
docker ps >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ERRORE: Docker non è in esecuzione
    echo    Avvia Docker Desktop e riprova
    echo.
    pause
    exit /b 1
)

echo ✅ Docker is running
echo.

REM ========== BUILD E AVVIO ==========

echo ================================================
echo 🔨 Build immagine Docker...
echo    (Può richiedere 2-3 minuti la prima volta)
echo ================================================
echo.

REM Rileva quale comando Docker Compose è disponibile
docker compose version >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "COMPOSE_CMD=docker compose"
) else (
    docker-compose --version >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        set "COMPOSE_CMD=docker-compose"
    ) else (
        echo ❌ ERRORE: Docker Compose non trovato
        echo    Installa Docker Desktop (include Compose)
        pause
        exit /b 1
    )
)

echo ✅ Usando: %COMPOSE_CMD%
echo.

REM Build e avvio con il comando rilevato
%COMPOSE_CMD% up --build -d

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERRORE: Build Docker fallita!
    echo    Verifica i log sopra per dettagli
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================
echo ✅ Applicazione avviata in background!
echo ================================================
echo.
echo L'applicazione sarà disponibile su:
echo   🌐 Homepage:     http://localhost:5000
echo   💾 H2 Console:   http://localhost:5000/h2-console
echo   ❤️  Health Check: http://localhost:5000/actuator/health
echo.
echo Comandi utili:
echo   - Visualizza logs:    %COMPOSE_CMD% logs -f
echo   - Verifica health:    %COMPOSE_CMD% ps
echo   - Ferma applicazione: %COMPOSE_CMD% down
echo   - Restart:            %COMPOSE_CMD% restart
echo.
echo ⏳ Attendi ~40 secondi per l'avvio completo
echo    (health check start-period configurato)
echo.
pause
