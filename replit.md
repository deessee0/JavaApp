# Padel App - Progetto Universitario

## Overview
Applicazione Spring Boot Java per la gestione di partite di padel, sviluppata come progetto universitario. L'applicazione implementa l'architettura MVC con 3 Design Pattern obbligatori (Observer, Strategy, Singleton) e fornisce un'interfaccia web semplificata per demo rapida.

## Autore
**Studente**: [Nome da inserire]  
**Corso**: Ingegneria del Software  
**Anno Accademico**: 2024/2025

## Architettura Tecnica
- **Framework**: Spring Boot 3.5.5
- **Language**: Java 17
- **Build Tool**: Maven with Maven Wrapper (./mvnw)
- **Database**: H2 in-memory database (development)
- **Web Server**: Embedded Tomcat on port 5000
- **Testing**: JUnit 5 + Spring Boot Test + JaCoCo
- **Template Engine**: Thymeleaf
- **Dependencies**: Spring Web, Spring Data JPA, Spring Boot Actuator, H2 Database, Lombok, Validation

## Design Patterns Implementati

### 1. Observer Pattern
- **Evento**: `MatchConfirmedEvent` (quando una partita raggiunge 4 giocatori)
- **Evento**: `MatchFinishedEvent` (quando una partita termina)
- **Listener**: `MatchEventListener` (ascolta eventi e invia notifiche)
- **Publisher**: `ApplicationEventPublisher` (Spring built-in)

### 2. Strategy Pattern
- **Interfaccia**: `MatchSortingStrategy`
- **Concrete Strategies**:
  - `DateSortingStrategy` - ordina per data
  - `PopularitySortingStrategy` - ordina per numero iscritti
  - `LevelSortingStrategy` - ordina per livello richiesto
- **Context**: `MatchService` (seleziona strategia dinamicamente)

### 3. Singleton Pattern
- Implementato tramite Spring IoC Container
- Tutti i Service beans sono Singleton per default
- `ApplicationEventPublisher` gestito come Singleton da Spring

## Funzionalità Principali
1. **Gestione Utenti**: Registrazione con livello dichiarato e livello percepito
2. **Gestione Partite**: Creazione, join, leave, conferma automatica a 4 giocatori
3. **Sistema Feedback**: Valutazione giocatori post-partita con aggiornamento livello percepito
4. **Sorting Dinamico**: Ordinamento partite con Strategy Pattern
5. **Notifiche**: Sistema notifiche tramite Observer Pattern

## Testing e Coverage

### Test Implementati (21 totali)
- **CompleteServiceTest** (14 test scenario-based):
  - testCreateMatch
  - testJoinMatch
  - testAutoConfirmAt4Players ⭐
  - testMaxPlayersLimit
  - testDuplicateRegistrationConstraint
  - testLeaveMatch
  - testCreateFeedback
  - testOneFeedbackPerUserPerMatch
  - testPerceivedLevelUpdate
  - testStrategyDateSorting
  - testStrategyPopularitySorting
  - testStrategyLevelSorting
  - testFilterByStatus
  - testFilterByLevel

- **StrategyPatternTest** (6 test):
  - Test per ogni strategia di sorting

- **PadelAppApplicationTests** (1 test):
  - Context load test

### Coverage JaCoCo
- **Instructions**: 56.7% (1023/1803)
- **Branches**: 26.2% (16/61)
- **Lines**: 54.1% (223/412)

**Nota**: Coverage sotto l'obiettivo 80% ma con test funzionali e scenario-based completi.

## Documentazione

### File Documentazione Completa
- `README.md` - Descrizione progetto, requisiti, istruzioni
- `docs/ARCHITECTURE.md` - Descrizione architettura e pattern
- `docs/use-case-diagram.puml` - Diagramma casi d'uso (PlantUML)
- `docs/class-diagram.puml` - Diagramma classi con pattern (PlantUML)
- `docs/sequence-diagram.puml` - Diagrammi sequenza flussi chiave (PlantUML)

## UI Semplificata

L'interfaccia è stata progettata per essere "demostrabile rapidamente" come richiesto:

### Caratteristiche UI
- **Homepage minimalista**: 3 CTA principali (Vedi Partite, Crea Partita, Gestisci Utenti)
- **Navigazione ridotta**: Solo 3 link essenziali (Home, Partite, Crea Partita)
- **Design pulito**: CSS ottimizzato con spazio bianco, colori neutri
- **Filtro singolo**: Semplice dropdown per filtrare partite
- **Azioni immediate**: Pulsanti Join/Leave funzionanti direttamente

## Configurazione Ambiente

### Development Setup
- Configurato per Replit environment con port 5000
- Server address: 0.0.0.0 (compatibile con proxy Replit)
- Forward headers strategy per iframe compatibility
- H2 console: `/h2-console`
- Spring Boot DevTools: hot reload abilitato
- Actuator: endpoint `/actuator`

### Development Workflow
- **Workflow**: "Spring Boot Server" runs `./mvnw spring-boot:run`
- **Port**: 5000 (configured for frontend access)
- **Auto-reload**: Enabled via Spring Boot DevTools

### Deployment
- **Target**: Autoscale (stateless web application)
- **Command**: `./mvnw spring-boot:run`
- Suitable for REST API services

## Modifiche Tecniche Importanti

### Fix Auto-Conferma Partite
- **Problema**: `Match.getActiveRegistrationsCount()` usava entity collection stale
- **Soluzione**: `MatchService` ora usa `RegistrationRepository.countActiveRegistrationsByMatch(match)`
- **Risultato**: Auto-conferma funziona correttamente quando 4° giocatore si unisce

### Test Scenario-Based
- Rimossi test superficiali con solo `assertNotNull`
- Creati test che validano scenari reali di business logic
- Test Spring Boot integration con @SpringBootTest

## Recent Changes
- 2025-10-14: **FIX THYMELEAF LAMBDA & LAZY LOADING** - Risolti errori template profilo
  - WebController: Calcolo distribuzione feedback nel controller (countPrincipiante, countIntermedio, etc.)
  - my-profile.html: Rimossi lambda expressions incompatibili con Thymeleaf SpEL
  - FeedbackRepository: Aggiunto JOIN FETCH per match oltre ad author e targetUser
  - Pagina "Il Mio Profilo" completamente funzionante con analytics feedback
- 2025-10-11: **FIX LAZY LOADING** - Risolti tutti i LazyInitializationException
  - FeedbackRepository: Aggiunto JOIN FETCH per author e targetUser
  - WebController: Aggiunto @Transactional(readOnly = true) su myMatches
  - my-matches.html: Rimosso contatore giocatori (causava lazy loading)
  - Applicazione completamente funzionante senza errori
- 2025-10-11: **RISTRUTTURAZIONE COMPLETA PER UTENTE SINGOLO (Margherita Biffi)** 
  - Backend: Creato UserContext per simulare utente loggato (Margherita Biffi)
  - Backend: WebController usa UserContext, rimossa selezione utente da tutti i form
  - Backend: Creazione partita auto-iscrive il creatore
  - UI: Home mostra partite disponibili (non iscritte) con pulsante "Iscriviti"
  - UI: Nuova pagina "Le Mie Partite" con iscritte/giocate/feedback
  - UI: Form Crea Partita semplificato, auto-iscrizione
  - UI: Form Feedback senza selezione autore (sempre Margherita)
  - DataSeeder: Dati completi per Margherita (partite giocate + feedback)
- 2025-10-11: **COMPLETAMENTO UI** - Sistemati tutti i bug UI e semplificata architettura
  - Fix form Crea Partita: aggiunto matchRequest al Model + POST handler in WebController
  - Architettura semplificata: eliminato MatchController REST ridondante, tutto in WebController
  - Fix filtro partite: corretto per usare `level` invece di `status` nel template
  - Pagina feedback completa: form funzionante per dare feedback post-partita
  - Pulsante "Termina Partita": aggiunto per partite confermate
  - Pagina utenti migliorata: confronto visuale dichiarato vs percepito con indicatori (✓↑↓)
- 2025-10-10: **FASE 1** - UI semplificata e ottimizzata per demo rapida
- 2025-10-10: **FASE 2** - Documentazione completa con UML diagrams
- 2025-10-10: **FASE 3** - Unit test scenario-based con coverage 56.7%
- 2025-10-10: Fix MatchService auto-conferma usando repository count
- 2025-10-10: Creazione CompleteServiceTest con 14 test reali

## Database Schema

### Entità JPA
- **User**: username, email, firstName, lastName, password, declaredLevel, perceivedLevel
- **Match**: location, dateTime, requiredLevel, type, status, creator
- **Registration**: user, match, status, registeredAt
- **Feedback**: author, targetUser, match, suggestedLevel, comment

### Relazioni
- User 1-N Match (creator)
- User N-N Match (via Registration)
- User N-N User (via Feedback per match)

## Come Eseguire

### Run Application
```bash
./mvnw spring-boot:run
```

### Run Tests
```bash
./mvnw test
```

### Generate Coverage Report
```bash
./mvnw test jacoco:report
# Report disponibile in: target/site/jacoco/index.html
```

### View H2 Console
- URL: http://localhost:5000/h2-console
- JDBC URL: jdbc:h2:mem:padeldb
- Username: sa
- Password: (vuoto)

## Stato Progetto

✅ **APPLICAZIONE COMPLETA E FUNZIONANTE**

L'applicazione è pronta per la submission universitaria:
- Design Patterns implementati correttamente (Observer, Strategy, Singleton)
- Simulazione utente singolo (Margherita Biffi) funzionante
- UI semplificata per demo rapida
- Tutti i LazyInitializationException risolti
- Test scenario-based completi (21 test)
- Documentazione UML completa

## Prossimi Step Suggeriti

1. **Aumentare Coverage**: Aggiungere test per controller e raggiungere 80%
2. **Screenshot**: Catturare evidenze per submission universitaria
3. **Presentation**: Preparare slide per dimostrazione pattern e architettura
4. **Integration Test**: Aggiungere test UI per my-matches con feedback

## Note Tecniche
- Database H2 in-memory: dati reset ad ogni restart
- Data seeding automatico: 6 utenti, 4 partite, 12 registrazioni
- LazyInitializationException fixes: JOIN FETCH nelle query repository
- Hibernate schema: auto DDL per sviluppo
