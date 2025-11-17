@echo off
REM Script per eseguire test suite completa e generare report coverage (Windows)
REM Uso: scripts\run-tests.bat

echo ================================================
echo 🧪 Esecuzione Test Suite - Padel App
echo ================================================
echo.

REM ========== ESECUZIONE TEST ==========

echo 📝 Esecuzione di tutti i test (59 test totali)...
echo.

call mvnw.cmd test

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERRORE: Alcuni test sono falliti!
    echo.
    pause
    exit /b 1
)

echo.
echo ================================================
echo ✅ Test completati con successo!
echo ================================================
echo.

REM ========== GENERAZIONE REPORT COVERAGE ==========

echo 📊 Generazione report JaCoCo Coverage...
echo.

call mvnw.cmd jacoco:report

echo.
echo ================================================
echo ✅ Report Coverage generato!
echo ================================================
echo.
echo 📈 Report HTML disponibile in:
echo    target\site\jacoco\index.html
echo.
echo Per visualizzare il report:
echo    start target\site\jacoco\index.html
echo.
echo Coverage attuale (circa):
echo   - Instruction: ~54.8%%
echo   - Line:        ~54.2%%
echo   - Branch:      ~29.4%%
echo.
echo 📌 Note:
echo    Il coverage è focalizzato su business logic e design patterns,
echo    non su presentation layer (WebController non testato).
echo.
pause
