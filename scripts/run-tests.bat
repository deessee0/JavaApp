@echo off
REM Script "Smart" per esecuzione test (Windows)
REM Tenta di eseguire i test usando Docker (metodo preferito).
REM Se Docker non è disponibile, prova ad usare Java locale.
REM Uso: scripts\run-tests.bat

REM Cambia alla directory root del progetto
cd /d "%~dp0\.."

set TEST_OUTPUT_FILE=test_output.log

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
REM Usiamo un trucco con powershell per avere tee su windows se vogliamo vedere l'output live E salvarlo
REM Ma per semplicità su bat standard, redirigiamo e basta, o usiamo type alla fine.
REM Per evitare complessità eccessiva su Windows senza strumenti esterni:
REM Eseguiamo il comando e redirigiamo tutto su file, poi mostriamo il file.
REM Svantaggio: l'utente non vede il progresso live.
REM Alternativa: Powershell Tee-Object.
REM Aggiunto --no-cache per forzare output

powershell -Command "docker build --target test --no-cache --progress=plain . 2>&1 | Tee-Object -FilePath '%TEST_OUTPUT_FILE%'"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERRORE: Test su Docker falliti!
    REM Non usciamo, mostriamo summary
) else (
    echo.
    echo ✅ Test su Docker completati con successo!
)
goto :PRINT_SUMMARY

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

powershell -Command "call '%~dp0\..\mvnw.cmd' test jacoco:report 2>&1 | Tee-Object -FilePath '%TEST_OUTPUT_FILE%'"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERRORE: Test locali falliti!
) else (
    echo.
    echo ✅ Test locali completati con successo!
)

:PRINT_SUMMARY
echo.
echo ================================================
echo 📊 RIASSUNTO TEST
echo ================================================

REM Estrai statistiche dai log
if exist "%TEST_OUTPUT_FILE%" (
    REM Cerca l'ultima riga che inizia con "Tests run:"
    for /f "delims=" %%i in ('findstr /C:"Tests run:" "%TEST_OUTPUT_FILE%"') do set SUMMARY_LINE=%%i
)

if defined SUMMARY_LINE (
    echo %SUMMARY_LINE%
) else (
    echo ⚠️  Impossibile trovare il riepilogo dei test nel log.
)

REM Gestione JaCoCo Coverage
set JACOCO_CSV=target\site\jacoco\jacoco.csv
if exist "%JACOCO_CSV%" (
    REM Usa PowerShell per calcolare la percentuale
    powershell -Command "$csv = Import-Csv '%JACOCO_CSV%'; $covered = ($csv | Measure-Object -Property INSTRUCTION_COVERED -Sum).Sum; $missed = ($csv | Measure-Object -Property INSTRUCTION_MISSED -Sum).Sum; $total = $covered + $missed; if ($total -gt 0) { $pct = ($covered / $total) * 100; Write-Host '📈 Copertura Istruzioni (JaCoCo):' $pct.ToString('N2')'%' } else { Write-Host '📈 Copertura Istruzioni (JaCoCo): 0%' }"
    echo 📄 Report completo: target\site\jacoco\index.html
) else (
    echo ⚠️  Report JaCoCo non trovato.
)

echo ================================================
if exist "%TEST_OUTPUT_FILE%" del "%TEST_OUTPUT_FILE%"

echo.
echo ================================================
echo 🎉 Suite di test terminata
echo ================================================
echo.
pause
