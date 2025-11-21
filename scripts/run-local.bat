@echo off
REM Script di avvio rapido per Padel App (Windows)
REM Uso: scripts\run-local.bat

REM Cambia alla directory root del progetto (una directory sopra scripts/)
cd /d "%~dp0\.."

echo ================================================
echo 🚀 Avvio Padel App (metodo locale con Maven)
echo ================================================
echo.

REM ========== VERIFICA PREREQUISITI ==========

echo 📋 Verifica prerequisiti...
echo.

REM Check Java
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ERRORE: Java non trovato nel sistema
    echo.
    echo 💡 SUGGERIMENTO:
    echo    Se non hai Java installato, usa lo script Docker:
    echo    scripts\run-docker.bat
    echo.
    pause
    exit /b 1
)

REM Check Maven wrapper (ora nella root del progetto)

REM Check Maven wrapper (ora nella root del progetto)
if not exist "mvnw.cmd" (
    echo ❌ ERRORE: Maven wrapper (mvnw.cmd) non trovato
    echo    Directory corrente: %CD%
    echo.
    pause
    exit /b 1
)

echo ✅ Maven wrapper trovato
echo.

REM ========== AVVIO APPLICAZIONE ==========

echo ================================================
echo 📦 Avvio Spring Boot Application...
echo ================================================
echo.
echo L'applicazione sarà disponibile su:
echo   🌐 Homepage:     http://localhost:5000
echo   💾 H2 Console:   http://localhost:5000/h2-console
echo   ❤️  Health Check: http://localhost:5000/actuator/health
echo.
echo Utente demo: margherita.biffi@padel.it / password123
echo.
echo Premi CTRL+C per fermare l'applicazione
echo ================================================
echo.

REM Avvia applicazione con Maven wrapper
call mvnw.cmd spring-boot:run
