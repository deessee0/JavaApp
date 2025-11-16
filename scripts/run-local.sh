#!/bin/bash

# Script di avvio rapido per Padel App
# Uso: ./scripts/run-local.sh

set -e  # Exit on error

echo "================================================"
echo "🚀 Avvio Padel App (metodo locale con Maven)"
echo "================================================"
echo ""

# ========== VERIFICA PREREQUISITI ==========

echo "📋 Verifica prerequisiti..."
echo ""

# Check Java version
if ! command -v java &> /dev/null; then
    echo "❌ ERRORE: Java non trovato nel sistema"
    echo "   Installa Java 17 o superiore:"
    echo "   - macOS: brew install openjdk@17"
    echo "   - Ubuntu: sudo apt install openjdk-17-jdk"
    echo "   - Windows: Scarica da https://adoptium.net"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ ERRORE: Java 17 o superiore richiesto"
    echo "   Versione attuale: Java $JAVA_VERSION"
    echo "   Aggiorna Java a versione 17+"
    exit 1
fi

echo "✅ Java version: $JAVA_VERSION"
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
