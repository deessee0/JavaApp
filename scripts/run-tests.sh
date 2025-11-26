#!/bin/bash

# Script "Smart" per esecuzione test
# Tenta di eseguire i test usando Docker (metodo preferito).
# Se Docker non è disponibile, prova ad usare Java locale.
# Uso: ./scripts/run-tests.sh

set -e  # Exit on error

# Cambia alla directory root del progetto
cd "$(dirname "$0")/.."

TEST_OUTPUT_FILE="test_output.log"

echo "================================================"
echo "🧪 Esecuzione Test Suite - Padel App"
echo "================================================"
echo ""

# Funzione per stampare il riassunto
print_summary() {
    echo ""
    echo "================================================"
    echo "📊 RIASSUNTO TEST"
    echo "================================================"
    
    # Estrai statistiche dai log di Maven
    SUMMARY_LINE=""
    if [ -f "$TEST_OUTPUT_FILE" ]; then
        # Cerca la riga finale "Tests run: X, Failures: Y, Errors: Z, Skipped: W"
        SUMMARY_LINE=$(grep "Tests run:" "$TEST_OUTPUT_FILE" | tail -n 1)
    fi

    # Se non trovato nel log (es. cached build), prova a calcolarlo dai report surefire
    if [ -z "$SUMMARY_LINE" ] && [ -d "target/surefire-reports" ]; then
        SUMMARY_LINE=$(grep -h "Tests run:" target/surefire-reports/*.txt 2>/dev/null | awk -F'[:,]' '{
            run += $2
            fail += $4
            err += $6
            skip += $8
        } END {
            print "Tests run: " run ", Failures: " fail ", Errors: " err ", Skipped: " skip
        }')
    fi

    if [ -n "$SUMMARY_LINE" ]; then
        echo "$SUMMARY_LINE"
    else
        echo "⚠️  Impossibile trovare il riepilogo dei test (log mancante o cache senza report)."
    fi

    # Gestione JaCoCo Coverage
    JACOCO_CSV="target/site/jacoco/jacoco.csv"
    if [ -f "$JACOCO_CSV" ]; then
        # Calcola la copertura delle istruzioni (Instruction Coverage)
        # Il CSV ha header: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,...
        # Colonne 4 e 5 sono missed e covered instructions (1-based index in awk)
        
        COVERAGE_STATS=$(awk -F, 'NR>1 {missed+=$4; covered+=$5} END {if ((missed+covered) > 0) printf "%.2f%%", (covered/(missed+covered))*100; else print "0%"}' "$JACOCO_CSV")
        
        echo "📈 Copertura Istruzioni (JaCoCo): $COVERAGE_STATS"
        echo "📄 Report completo: target/site/jacoco/index.html"
    else
        echo "⚠️  Report JaCoCo non trovato."
    fi
    
    echo "================================================"
    # rm -f "$TEST_OUTPUT_FILE"
}

# Funzione per eseguire test con Docker
run_docker_tests() {
    echo "🐳 Docker rilevato! Esecuzione test nel container..."
    echo "   (Questo garantisce un ambiente pulito e isolato)"
    echo ""
    
    # Build fino al target 'test'
    # --progress=plain mostra l'output dei test in console
    # Usiamo tee per mostrare l'output E salvarlo su file per il parsing
    # Aggiungiamo --no-cache per forzare l'esecuzione dei test e vedere l'output
    set +e # Disabilita exit on error temporaneamente per catturare fallimenti test
    DOCKER_BUILDKIT=1 docker build --target test -t padel-test-image --progress=plain . 2>&1 | tee "$TEST_OUTPUT_FILE"
    EXIT_CODE=${PIPESTATUS[0]}
    set -e # Riabilita
    
    if [ $EXIT_CODE -eq 0 ]; then
        # Estrai il report di coverage e i report dei test (surefire)
        id=$(docker create padel-test-image)
        mkdir -p target/site/jacoco
        mkdir -p target/surefire-reports
        docker cp $id:/app/target/site/jacoco/jacoco.csv target/site/jacoco/jacoco.csv 2>/dev/null || echo "⚠️ Impossibile estrarre report coverage"
        docker cp $id:/app/target/surefire-reports/. target/surefire-reports/ 2>/dev/null || echo "⚠️ Impossibile estrarre report test"
        docker rm -v $id >/dev/null
    fi
    
    echo ""
    if [ $EXIT_CODE -eq 0 ]; then
        echo "✅ Test su Docker completati con successo!"
    else
        echo "❌ Test su Docker falliti!"
        # Non usciamo subito, vogliamo stampare il summary
    fi
}

# Funzione per eseguire test locali
run_local_tests() {
    echo "☕ Tentativo esecuzione locale (richiede Java)..."
    
    if ! ./mvnw -version &> /dev/null; then
        echo "❌ ERRORE: Java/Maven non trovato."
        echo "   Per eseguire i test serve Docker (consigliato) oppure Java 17+ installato."
        exit 1
    fi
    
    set +e
    ./mvnw test jacoco:report | tee "$TEST_OUTPUT_FILE"
    EXIT_CODE=${PIPESTATUS[0]}
    set -e
    
    echo ""
    if [ $EXIT_CODE -eq 0 ]; then
        echo "✅ Test locali completati con successo!"
    else
        echo "❌ Test locali falliti!"
    fi
}

# Logica di selezione
if command -v docker &> /dev/null; then
    run_docker_tests
else
    echo "⚠️  Docker non trovato."
    run_local_tests
fi

print_summary

# Esci con il codice corretto se i test sono falliti
if grep -q "BUILD FAILURE" "$TEST_OUTPUT_FILE" 2>/dev/null; then
    exit 1
fi
