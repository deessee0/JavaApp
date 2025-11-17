@echo off
REM Script di avvio rapido per Padel App (Windows)
REM Uso: scripts\run-local.bat

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
    echo    Installa Java 17 o superiore:
    echo    - Windows: Scarica da https://adoptium.net
    echo.
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%v
)
set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "tokens=1 delims=." %%a in ("%JAVA_VERSION%") do set JAVA_MAJOR=%%a

if %JAVA_MAJOR% LSS 17 (
    echo ❌ ERRORE: Java 17 o superiore richiesto
    echo    Versione attuale: Java %JAVA_MAJOR%
    echo    Aggiorna Java a versione 17+
    echo.
    pause
    exit /b 1
)

echo ✅ Java version: %JAVA_MAJOR%
echo.

REM Check Maven wrapper
if not exist "mvnw.cmd" (
    echo ❌ ERRORE: Maven wrapper (mvnw.cmd) non trovato
    echo    Assicurati di essere nella root del progetto
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
mvnw.cmd spring-boot:run
