# 🚀 Quick Start - Padel App

**Guida rapida per avviare l'applicazione in meno di 2 minuti.**

---

## ⚡ Avvio Immediato (Metodo Consigliato)

### Prerequisiti
- **Java 17** o superiore ([Download Java](https://adoptium.net))
- **Maven** (oppure usa il wrapper `./mvnw` incluso nel progetto)

### Comandi

```bash
# 1. Clona il repository (se necessario)
git clone <URL-REPOSITORY>
cd padel-app

# 2. Avvia l'applicazione con script automatico
chmod +x scripts/run-local.sh
./scripts/run-local.sh

# OPPURE manualmente con Maven wrapper
./mvnw spring-boot:run
```

✅ **L'applicazione sarà disponibile su**: http://localhost:5000

⏱️ **Tempo di avvio**: ~15-20 secondi  
✅ **Database H2**: Pre-popolato automaticamente con dati demo

---

## 🌐 Accesso All'Applicazione

Dopo l'avvio, apri il browser su:

| Risorsa | URL | Descrizione |
|---------|-----|-------------|
| **Homepage** | http://localhost:5000 | Interfaccia principale |
| **H2 Console** | http://localhost:5000/h2-console | Database console (debug) |
| **Health Check** | http://localhost:5000/actuator/health | Status applicazione |

### Credenziali H2 Database Console

```
JDBC URL:  jdbc:h2:mem:padeldb
Username:  sa
Password:  (lascia vuoto)
```

---

## 👤 Utente Demo

L'applicazione **simula un utente già loggato**:

- **Nome**: Margherita Biffi
- **Email**: margherita.biffi@example.com
- **Livello**: Intermedio

Non è necessaria alcuna registrazione o login!

---

## 📊 Dati Pre-Caricati

Al primo avvio, il database viene popolato automaticamente con:

- ✅ **10 utenti** demo
- ✅ **15 partite** in vari stati (WAITING, CONFIRMED, FINISHED)
- ✅ **Iscrizioni** e **feedback** per testing completo

---

## 🎯 Funzionalità da Testare Durante la Demo

### 1. Homepage
- Visualizza statistiche partite e utenti
- Mostra partite disponibili

### 2. Crea Nuova Partita
- Naviga su "Crea Partita"
- Compila il form (tipo, livello, data/ora, location)
- Crea partita → diventa automaticamente il creatore

### 3. Join Match (Observer Pattern)
- Cerca una partita con status WAITING
- Clicca "Iscriviti"
- **Al 4° giocatore** → partita auto-confermata con evento Observer

### 4. Strategy Pattern - Ordinamento
Nella pagina `/matches`, prova i 3 ordinamenti:
- **Per Data**: `/matches?sort=date`
- **Per Popolarità**: `/matches?sort=popularity`
- **Per Livello**: `/matches?sort=level`

### 5. Feedback Sistema
- Cerca una partita FINISHED dove sei iscritto
- Clicca "Lascia Feedback"
- Valuta gli altri 3 giocatori
- Il **perceived level** viene calcolato automaticamente

---

## 🧪 Esecuzione Test e Coverage

### Test Suite Completa (59 test)

```bash
# Esegui test con script automatico
chmod +x scripts/run-tests.sh
./scripts/run-tests.sh

# OPPURE manualmente
./mvnw test
```

### Report Coverage JaCoCo

```bash
# Genera report
./mvnw jacoco:report

# Visualizza report HTML
open target/site/jacoco/index.html      # macOS
xdg-open target/site/jacoco/index.html  # Linux
start target/site/jacoco/index.html     # Windows
```

**Coverage Attuale**:
- Instruction: ~54.8%
- Line: ~54.2%
- Branch: ~29.4%

> **Nota**: Il coverage è focalizzato su **business logic** e **design patterns**, non sul presentation layer (WebController intenzionalmente non testato).

---

## 🐳 Metodo Alternativo: Docker (Opzionale)

### Prerequisiti Docker
- Docker Desktop installato ([Download Docker](https://www.docker.com/products/docker-desktop))

### Avvio con Docker

```bash
# Metodo 1: Script automatico
chmod +x scripts/run-docker.sh
./scripts/run-docker.sh

# Metodo 2: Comandi manuali
docker-compose up --build -d

# Visualizza logs
docker-compose logs -f

# Stop applicazione
docker-compose down
```

✅ Applicazione disponibile su: http://localhost:5000

---

## 🛠️ Troubleshooting

### Problema: Porta 5000 già occupata

**Soluzione 1**: Libera la porta
```bash
# macOS/Linux
lsof -ti:5000 | xargs kill -9

# Windows
netstat -ano | findstr :5000
taskkill /PID <PID> /F
```

**Soluzione 2**: Cambia porta in `application.properties`
```properties
server.port=8080
```

### Problema: Java non trovato o versione sbagliata

```bash
# Verifica versione Java
java -version

# Dovrebbe mostrare: openjdk version "17.x.x" o superiore
```

Se hai versione < 17, installa Java 17:
- **macOS**: `brew install openjdk@17`
- **Ubuntu**: `sudo apt install openjdk-17-jdk`
- **Windows**: Scarica da https://adoptium.net

### Problema: ./mvnw command not found

```bash
# Rendi eseguibile il Maven wrapper
chmod +x mvnw

# Oppure usa Maven installato nel sistema
mvn spring-boot:run
```

---

## 📚 Documentazione Completa

Per documentazione dettagliata su architettura, design patterns e testing:
- **README.md**: Guida completa del progetto
- **ARCHITECTURE.md**: Dettagli architetturali e pattern
- **replit.md**: Documentazione tecnica e decisioni di design

---

## 🎓 Per la Consegna Universitaria

### Checklist Pre-Demo

- [ ] Applicazione si avvia senza errori
- [ ] Homepage carica correttamente
- [ ] Database H2 popolato con dati
- [ ] Test suite passa (59/59 test ✅)
- [ ] Coverage report generato
- [ ] Strategy Pattern dimostrabile (ordinamento matches)
- [ ] Observer Pattern funzionante (auto-conferma al 4° giocatore)

### Durante la Demo Orale

1. **Avvia app**: `./scripts/run-local.sh`
2. **Mostra homepage**: http://localhost:5000
3. **Dimostra Strategy**: Ordina matches per data/popolarità/livello
4. **Dimostra Observer**: Join match fino a 4 giocatori → auto-conferma
5. **Mostra test**: `./scripts/run-tests.sh` → coverage report

---

## ✅ Riepilogo Comandi Rapidi

```bash
# Avvio applicazione
./scripts/run-local.sh

# Esegui test
./scripts/run-tests.sh

# Avvio con Docker
./scripts/run-docker.sh

# Stop tutto
docker-compose down  # (se usi Docker)
CTRL+C               # (se usi Maven)
```

---

**Pronto per la consegna!** 🎉

Per domande o problemi, consulta la documentazione completa nel README.md.
