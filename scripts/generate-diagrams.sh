#!/bin/bash

# Script per generare diagrammi PlantUML usando Docker
# Usage: ./scripts/generate-diagrams.sh

# Directory base del progetto
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS_DIR="$PROJECT_DIR/docs"
OUTPUT_DIR="$DOCS_DIR/images"

# Colori per output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Avvio generazione diagrammi PlantUML...${NC}"

# Verifica Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker non trovato. Installalo per continuare.${NC}"
    exit 1
fi

# Crea directory output se non esiste
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "📁 Creazione directory output: $OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"
fi

# Esegui PlantUML via Docker
# Mounta la cartella docs in /data nel container
# -o images: specifica output relativo a /data (quindi docs/images)
echo "🎨 Rendering diagrammi in corso..."
docker run --rm \
    -e PLANTUML_LIMIT_SIZE=16384 \
    -v "$DOCS_DIR":/data \
    plantuml/plantuml \
    -tpng -o images \
    "*.puml"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Generazione completata!${NC}"
    echo "🖼️  Immagini salvate in: $OUTPUT_DIR"
    ls -l "$OUTPUT_DIR"
else
    echo -e "${RED}❌ Errore durante la generazione.${NC}"
    exit 1
fi
