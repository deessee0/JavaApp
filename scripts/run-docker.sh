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

# Check se Docker è in esecuzione
if ! docker ps &> /dev/null; then
    echo "❌ ERRORE: Docker non è in esecuzione"
    echo "   Avvia Docker Desktop/daemon e riprova"
    exit 1
fi

echo "✅ Docker is running"
echo ""

# ========== BUILD E AVVIO ==========

echo "================================================"
echo "🔨 Build immagine Docker..."
echo "   (Può richiedere 2-3 minuti la prima volta)"
echo "================================================"
echo ""

# Rileva quale comando Docker Compose è disponibile
if docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
else
    echo "❌ ERRORE: Docker Compose non trovato"
    echo "   Installa Docker Compose o usa Docker Desktop (include Compose)"
    exit 1
fi

echo "✅ Usando: $COMPOSE_CMD"
echo ""

# Build e avvio con il comando rilevato
$COMPOSE_CMD up --build -d

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
echo "  - Visualizza logs:    $COMPOSE_CMD logs -f"
echo "  - Verifica health:    $COMPOSE_CMD ps"
echo "  - Ferma applicazione: $COMPOSE_CMD down"
echo "  - Restart:            $COMPOSE_CMD restart"
echo ""
echo "⏳ Attendi ~40 secondi per l'avvio completo"
echo "   (health check start-period configurato)"
echo ""
