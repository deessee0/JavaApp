# 🚀 Script di Avvio Rapido - Padel App

Questa suite di script permette di avviare e testare l'applicazione in modo semplice, sia con Docker (consigliato) che con Java locale.

---

## 🐳 1. Metodo Consigliato (Docker)
**Ideale per:** Chi non ha Java installato o vuole un ambiente pulito.

### Avvio Applicazione
```bash
./scripts/run-docker.sh
```
- Costruisce l'immagine Docker ottimizzata.
- Avvia l'app sulla porta **5000**.
- Non richiede Java sul computer host.

### Esecuzione Test
```bash
./scripts/run-tests.sh
```
- Rileva automaticamente Docker ed esegue i test in un container isolato.
- Garantisce che i test girino nello stesso ambiente di produzione.

---

## ☕ 2. Metodo Alternativo (Java Locale)
**Ideale per:** Sviluppo rapido o chi ha già l'ambiente configurato.
**Prerequisiti:** Java 17+ installato.

### Avvio Applicazione
```bash
./scripts/run-local.sh
```
- Usa Maven Wrapper (`mvnw`) per avviare l'app.
- Se Java non è trovato, ti suggerirà di usare Docker.

### Esecuzione Test
```bash
./scripts/run-tests.sh
```
- Se Docker **NON** è installato, lo script proverà automaticamente ad usare Java locale.
- Genera report HTML in `target/site/jacoco/index.html`.

---

## 🔗 Accesso Applicazione
Una volta avviata (con Docker o Locale):

- **Homepage:** [http://localhost:5000](http://localhost:5000)
- **H2 Console:** [http://localhost:5000/h2-console](http://localhost:5000/h2-console)
- **Health Check:** [http://localhost:5000/actuator/health](http://localhost:5000/actuator/health)

**Credenziali Demo:**
- Email: `margherita.biffi@padel.it`
- Password: `password123`

---

## 🛠️ Note Tecniche per la Valutazione

### Architettura Docker
Il `Dockerfile` utilizza una **Multi-Stage Build** per efficienza e pulizia:
1.  **Base**: Cache delle dipendenze Maven.
2.  **Test**: Esegue la suite di test (`mvnw test`). Se fallisce, il build si ferma.
3.  **Build**: Compila il JAR finale (senza rieseguire i test).
4.  **Runtime**: Immagine leggera (`eclipse-temurin:17-jre-alpine`) per l'esecuzione.

### Script "Smart"
Lo script `run-tests.sh` è progettato per essere universale:
- **Priorità Docker**: Se Docker è presente, lo usa per garantire riproducibilità.
- **Fallback Java**: Se Docker manca, usa l'ambiente locale.

---

## 🐛 Troubleshooting

### "Permission denied" su Linux/Mac
Rendi eseguibili gli script:
```bash
chmod +x scripts/*.sh
```

### Porta 5000 occupata
Se l'avvio fallisce perché la porta è occupata:
1.  Trova il processo: `lsof -i :5000`
2.  Termina il processo o ferma il container precedente: `docker stop padel-app`

