#!/bin/bash

# Script di avvio rapido per Padel App
# Uso: ./scripts/run-local.sh

set -e  # Exit on error

# Cambia alla directory root del progetto (una directory sopra scripts/)
cd "$(dirname "$0")/.."

echo "================================================"
echo "☕ Avvio Locale Padel App"
echo "================================================"
echo ""

# Verifica presenza Java
if ! ./mvnw -version &> /dev/null; then
    echo "❌ ERRORE: Java non trovato o non configurato correttamente."
    echo ""
    echo "💡 SUGGERIMENTO:"
    echo "   Se non hai Java installato, usa lo script Docker:"
    echo "   ./scripts/run-docker.sh"
    echo ""
    exit 1
fi

echo "✅ Java rilevato, avvio applicazione..."
echo ""

# ========== VERIFICA PREREQUISITI ==========

echo "📋 Verifica prerequisiti..."
echo ""

# Check Maven wrapper
if [ ! -f "./mvnw" ]; then
    echo "❌ ERRORE: Maven wrapper (mvnw) non trovato"
    echo "   Assicurati di essere nella root del progetto"
    exit 1
fi

echo "✅ Maven wrapper trovato"
echo ""

# Check Maven wrapper
if [ ! -f "./mvnw" ]; then
    echo "❌ ERRORE: Maven wrapper (mvnw) non trovato"
    echo "   Assicurati di essere nella root del progetto"
    exit 1
fi

echo "✅ Maven wrapper trovato"
echo ""

# ========== AVVIO APPLICAZIONE ==========

echo "================================================"
echo "📦 Avvio Spring Boot Application..."
echo "================================================"
echo ""
echo "L'applicazione sarà disponibile su:"
echo "  🌐 Homepage:     http://localhost:5000"
echo "  💾 H2 Console:   http://localhost:5000/h2-console"
echo "  ❤️  Health Check: http://localhost:5000/actuator/health"
echo ""
echo "Utente simulato: Margherita Biffi (auto-login)"
echo ""
echo "Premi CTRL+C per fermare l'applicazione"
echo "================================================"
echo ""

# Avvia applicazione con Maven wrapper
./mvnw spring-boot:run
