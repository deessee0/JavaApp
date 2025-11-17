# 🚀 Script di Avvio Rapido - Padel App

Script multi-piattaforma (Linux/Mac/Windows) per avviare e testare facilmente l'applicazione.

## 📋 Script Disponibili

### 1. **Avvio Locale** (consigliato per sviluppo)
Avvia l'app usando Maven wrapper (richiede Java 17+).

**Linux/Mac:**
```bash
./scripts/run-local.sh
```

**Windows:**
```cmd
scripts\run-local.bat
```

**Prerequisiti:**
- Java 17 o superiore
- Maven wrapper (incluso nel progetto)

**Accesso:**
- Homepage: http://localhost:5000
- H2 Console: http://localhost:5000/h2-console
- Health Check: http://localhost:5000/actuator/health

**Credenziali demo:**
- Email: `margherita.biffi@padel.it`
- Password: `password123`

---

### 2. **Esecuzione Test**
Esegue tutti i 59 test e genera report coverage JaCoCo.

**Linux/Mac:**
```bash
./scripts/run-tests.sh
```

**Windows:**
```cmd
scripts\run-tests.bat
```

**Output:**
- Report HTML: `target/site/jacoco/index.html`
- Coverage attuale: ~54% (focalizzato su business logic)

**Aprire il report:**
- **macOS:** `open target/site/jacoco/index.html`
- **Linux:** `xdg-open target/site/jacoco/index.html`
- **Windows:** `start target\site\jacoco\index.html`

---

### 3. **Avvio con Docker** (per deployment production-ready)
Build e avvia l'app in container Docker isolato.

**Linux/Mac:**
```bash
./scripts/run-docker.sh
```

**Windows:**
```cmd
scripts\run-docker.bat
```

**Prerequisiti:**
- Docker Desktop (include Docker Compose)
- Docker daemon in esecuzione

**Comandi utili:**
```bash
# Nota: Usa "docker compose" (nuovo) o "docker-compose" (vecchio) a seconda della tua installazione
# Lo script rileva automaticamente quale è disponibile

# Visualizza logs
docker compose logs -f  # oppure: docker-compose logs -f

# Verifica stato container
docker compose ps  # oppure: docker-compose ps

# Ferma applicazione
docker compose down  # oppure: docker-compose down

# Restart
docker compose restart  # oppure: docker-compose restart
```

⏳ **Nota:** L'app impiega ~40 secondi per l'avvio completo (health check configurato).

---

## 🛠️ Troubleshooting

### ❌ "Java non trovato"
**Soluzione:** Installa Java 17 o superiore:
- **Windows:** https://adoptium.net/
- **macOS:** `brew install openjdk@17`
- **Ubuntu/Debian:** `sudo apt install openjdk-17-jdk`

Verifica installazione:
```bash
java -version
```

---

### ❌ "Docker non è in esecuzione"
**Soluzione:** 
1. Avvia Docker Desktop
2. Attendi che l'icona Docker diventi verde
3. Riprova lo script

Verifica:
```bash
docker ps
```

---

### ❌ "Permission denied" (Linux/Mac)
**Soluzione:** Rendi eseguibili gli script:
```bash
chmod +x scripts/*.sh
```

---

### ❌ Build Docker fallita
**Soluzione:**
1. Verifica spazio disco disponibile (richiesti ~500MB)
2. Pulisci cache Docker:
   ```bash
   docker system prune -a
   ```
3. Riprova il build

---

## 📊 Coverage Report

Il coverage è **intenzionalmente** focalizzato su:
- ✅ Business logic (Service layer)
- ✅ Design patterns (Observer, Strategy, Singleton)
- ✅ Model validation e JPA entities

**Non testato:**
- ❌ Presentation layer (WebController) - focus su funzionalità core
- ❌ Template Thymeleaf - testati manualmente

**Statistiche attuali:**
- 59 test totali
- Instruction Coverage: ~54.8%
- Line Coverage: ~54.2%
- Branch Coverage: ~29.4%

---

## 🎯 Quick Start per Professori/Revisori

**Avvio in 30 secondi (con Java installato):**
```bash
# Linux/Mac
./scripts/run-local.sh

# Windows
scripts\run-local.bat
```

**Verifica funzionamento:**
1. Attendi messaggio "Started PadelAppApplication"
2. Apri http://localhost:5000
3. Login con: `margherita.biffi@padel.it` / `password123`

**Test automatici:**
```bash
# Linux/Mac
./scripts/run-tests.sh

# Windows
scripts\run-tests.bat
```

Aspettati: **59/59 test passati** ✅

---

## 📝 Note Tecniche

- **Port 5000:** Configurato in `application.properties`
- **Database:** H2 in-memory (auto-popolato con dati demo)
- **Hot Reload:** Abilitato con Spring DevTools
- **Health Check:** Endpoint `/actuator/health` per monitoring
- **Docker:** Multi-stage build (JDK build → JRE runtime, ~150MB finale)

---

## 🐛 Segnalazione Bug

Se riscontri problemi:
1. Verifica prerequisiti (Java/Docker versioni)
2. Leggi i log di errore completi
3. Controlla `QUICKSTART.md` per troubleshooting dettagliato
