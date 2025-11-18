#!/bin/bash

# Script per eseguire test suite completa e generare report coverage
# Uso: ./scripts/run-tests.sh

set -e  # Exit on error

# Cambia alla directory root del progetto (una directory sopra scripts/)
cd "$(dirname "$0")/.."

echo "================================================"
echo "🧪 Esecuzione Test Suite - Padel App"
echo "================================================"
echo ""

# ========== ESECUZIONE TEST ==========

echo "📝 Esecuzione di tutti i test (59 test totali)..."
echo ""

./mvnw test

echo ""
echo "================================================"
echo "✅ Test completati con successo!"
echo "================================================"
echo ""

# ========== GENERAZIONE REPORT COVERAGE ==========

echo "📊 Generazione report JaCoCo Coverage..."
echo ""

./mvnw jacoco:report

echo ""
echo "================================================"
echo "✅ Report Coverage generato!"
echo "================================================"
echo ""
echo "📈 Report HTML disponibile in:"
echo "   target/site/jacoco/index.html"
echo ""
echo "Per visualizzare il report:"
echo ""
echo "  macOS:   open target/site/jacoco/index.html"
echo "  Linux:   xdg-open target/site/jacoco/index.html"
echo "  Windows: start target/site/jacoco/index.html"
echo ""
echo "Coverage attuale (circa):"
echo "  - Instruction: ~54.8%"
echo "  - Line:        ~54.2%"
echo "  - Branch:      ~29.4%"
echo ""
echo "📌 Note:"
echo "   Il coverage è focalizzato su business logic e design patterns,"
echo "   non su presentation layer (WebController non testato)."
echo ""
