#!/bin/bash

# Script di avvio rapido con Docker
# Uso: ./scripts/run-docker.sh

set -e  # Exit on error

echo "================================================"
echo "🐳 Avvio Padel App con Docker"
echo "================================================"
echo ""

# ========== VERIFICA PREREQUISITI ==========

echo "📋 Verifica prerequisiti..."
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ ERRORE: Docker non trovato"
    echo "   Installa Docker Desktop:"
    echo "   - macOS/Windows: https://www.docker.com/products/docker-desktop"
    echo "   - Linux: sudo apt install docker.io"
    exit 1
fi

echo "✅ Docker installato"
echo ""

# Check Docker Compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ ERRORE: Docker Compose non trovato"
    echo "   Installa Docker Compose o usa Docker Desktop (include Compose)"
    exit 1
fi

echo "✅ Docker Compose disponibile"
echo ""

# ========== BUILD E AVVIO ==========

echo "================================================"
echo "🔨 Build immagine Docker..."
echo "   (Può richiedere 2-3 minuti la prima volta)"
echo "================================================"
echo ""

docker-compose up --build -d

echo ""
echo "================================================"
echo "✅ Applicazione avviata in background!"
echo "================================================"
echo ""
echo "L'applicazione sarà disponibile su:"
echo "  🌐 Homepage:     http://localhost:5000"
echo "  💾 H2 Console:   http://localhost:5000/h2-console"
echo "  ❤️  Health Check: http://localhost:5000/actuator/health"
echo ""
echo "Comandi utili:"
echo "  - Visualizza logs:    docker-compose logs -f"
echo "  - Verifica health:    docker-compose ps"
echo "  - Ferma applicazione: docker-compose down"
echo "  - Restart:            docker-compose restart"
echo ""
echo "⏳ Attendi ~40 secondi per l'avvio completo"
echo "   (health check start-period configurato)"
echo ""
