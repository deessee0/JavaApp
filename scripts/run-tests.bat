@echo off
REM Script "Smart" per esecuzione test (Windows)
REM Tenta di eseguire i test usando Docker (metodo preferito).
REM Se Docker non è disponibile, prova ad usare Java locale.
REM Uso: scripts\run-tests.bat

REM Cambia alla directory root del progetto
cd /d "%~dp0\.."

echo ================================================
echo 🧪 Esecuzione Test Suite - Padel App
echo ================================================
echo.

REM Check Docker
where docker >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    goto :RUN_DOCKER
) else (
    echo ⚠️  Docker non trovato.
    goto :RUN_LOCAL
)

:RUN_DOCKER
echo 🐳 Docker rilevato! Esecuzione test nel container...
echo    (Questo garantisce un ambiente pulito e isolato)
echo.

REM Build fino al target 'test'
REM --progress=plain mostra l'output dei test in console
set DOCKER_BUILDKIT=1
docker build --target test --progress=plain .

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERRORE: Test su Docker falliti!
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ Test su Docker completati con successo!
goto :END

:RUN_LOCAL
echo ☕ Tentativo esecuzione locale (richiede Java)...

REM Check Java
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ERRORE: Java non trovato nel sistema.
    echo    Per eseguire i test serve Docker (consigliato) oppure Java 17+ installato.
    echo.
    pause
    exit /b 1
)

call "%~dp0\..\mvnw.cmd" test jacoco:report

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERRORE: Test locali falliti!
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ Test locali completati con successo!
echo 📊 Report: target\site\jacoco\index.html

:END
echo.
echo ================================================
echo 🎉 Suite di test terminata
echo ================================================
echo.
pause
