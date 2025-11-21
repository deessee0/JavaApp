#!/bin/bash

# Script "Smart" per esecuzione test
# Tenta di eseguire i test usando Docker (metodo preferito).
# Se Docker non è disponibile, prova ad usare Java locale.
# Uso: ./scripts/run-tests.sh

set -e  # Exit on error

# Cambia alla directory root del progetto
cd "$(dirname "$0")/.."

echo "================================================"
echo "🧪 Esecuzione Test Suite - Padel App"
echo "================================================"
echo ""

# Funzione per eseguire test con Docker
run_docker_tests() {
    echo "🐳 Docker rilevato! Esecuzione test nel container..."
    echo "   (Questo garantisce un ambiente pulito e isolato)"
    echo ""
    
    # Build fino al target 'test'
    # --progress=plain mostra l'output dei test in console
    DOCKER_BUILDKIT=1 docker build --target test --progress=plain .
    
    echo ""
    echo "✅ Test su Docker completati con successo!"
}

# Funzione per eseguire test locali
run_local_tests() {
    echo "☕ Tentativo esecuzione locale (richiede Java)..."
    
    if ! ./mvnw -version &> /dev/null; then
        echo "❌ ERRORE: Java/Maven non trovato."
        echo "   Per eseguire i test serve Docker (consigliato) oppure Java 17+ installato."
        exit 1
    fi
    
    ./mvnw test jacoco:report
    
    echo ""
    echo "✅ Test locali completati con successo!"
    echo "📊 Report: target/site/jacoco/index.html"
}

# Logica di selezione
if command -v docker &> /dev/null; then
    run_docker_tests
else
    echo "⚠️  Docker non trovato."
    run_local_tests
fi

echo ""
echo "================================================"
echo "🎉 Suite di test terminata"
echo "================================================"
echo ""
