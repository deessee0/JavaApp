# Architettura Software - App Padel

## Indice
1. [Panoramica Architetturale](#panoramica-architetturale)
2. [Pattern Architetturali](#pattern-architetturali)
3. [Design Patterns](#design-patterns)
4. [Decisioni di Design](#decisioni-di-design)
5. [Gestione Dati e Persistenza](#gestione-dati-e-persistenza)

---

## 1. Panoramica Architetturale

### Diagrammi UML del Progetto

La documentazione include i seguenti diagrammi UML, divisi per facilità di lettura e conformità ai requisiti:

#### 1.1 Class Diagrams

**Vista Architetturale:**
- `class-diagram-overview.puml` - Vista d'insieme packages e relazioni tra layer
- `class-diagram-complete.puml` - Diagramma completo (backup di riferimento)

**Diagrammi per Package:**
- `class-diagram-model.puml` - Model layer (entità di dominio e enum)
- `class-diagram-service.puml` - Service layer (logica business)
- `class-diagram-repository.puml` - Repository layer (accesso dati)
- `class-diagram-controller.puml` - Controller e Configuration layer (presentazione)

**Diagrammi Pattern:**
- `class-diagram-patterns.puml` - Design patterns implementati (Strategy, Observer, Singleton)

#### 1.2 Use Case Diagram

- `use-case-diagram.puml` - Casi d'uso e attori del sistema

#### 1.3 Sequence Diagrams

- `sequence-diagram.puml` - Flusso generale dell'applicazione
- `strategy-sequence-diagram.puml` - Interazione Strategy pattern
- `observer-sequence-diagram.puml` - Interazione Observer pattern

**Organizzazione per Requisiti Consegna:**

Ogni diagramma è stato progettato per aderire ai requisiti specificati nella consegna del progetto:

| Requisito Consegna | Diagramma Corrispondente | Descrizione |
|-------------------|--------------------------|-------------|
| Use case diagram con descrizione attori e funzionalità | `use-case-diagram.puml` | Mostra tutti i casi d'uso del sistema per Utente e Creator |
| Class diagram con descrizione packages | `class-diagram-overview.puml` | Vista architetturale con tutti i packages e le loro relazioni |
| Class diagram con descrizione classi principali per package | `class-diagram-model.puml`<br>`class-diagram-service.puml`<br>`class-diagram-repository.puml`<br>`class-diagram-controller.puml` | Dettaglio di ogni layer/package con tutte le classi, attributi e metodi |
| Class diagram con descrizione pattern utilizzati | `class-diagram-patterns.puml` | Documentazione dettagliata di Strategy, Observer e Singleton pattern |
| Sequence/activity/state diagram casi rilevanti | `sequence-diagram.puml`<br>`strategy-sequence-diagram.puml`<br>`observer-sequence-diagram.puml` | Flussi di interazione per casi d'uso principali e pattern |

**Pattern Documentati (oltre a MVC):**
- ✅ **Strategy Pattern** - Ordinamento partite con algoritmi intercambiabili
- ✅ **Observer Pattern** - Sistema eventi per notifiche match confirmed/finished
- ✅ **Singleton Pattern** - NotificationService (gestito da Spring)
- ✅ **Repository Pattern** - Astrazione accesso dati

*Nota: I pattern forniti nativamente da Spring Framework (Dependency Injection, IoC, Proxy) non sono conteggiati come pattern custom, in conformità con i requisiti.*

---

### Stack Tecnologico

**Backend**:
- Spring Boot 3.5.5
- Java 17
- Spring Data JPA
- Hibernate 6.6.26

**Database**:
- H2 Database (in-memory per development)
- Possibilità di migrazione a PostgreSQL/MySQL per production

**Frontend**:
- Thymeleaf Template Engine
- HTML5/CSS3
- Interfaccia responsive

**Testing**:
- JUnit 5
- Mockito
- JaCoCo (code coverage >85%)

### Principi Architetturali Applicati

1. **Separation of Concerns**: Netta separazione tra logica business, accesso dati e presentazione
2. **Dependency Injection**: Utilizzo del container IoC di Spring per gestione dipendenze
3. **Interface Segregation**: Interfacce dedicate per strategie e repository
4. **Single Responsibility**: Ogni classe ha una responsabilità ben definita

---

## 2. Pattern Architetturali

### 2.1 Model-View-Controller (MVC)

#### Model (Modello di Dominio)
**Package**: `com.example.padel_app.model`

Rappresenta le entità di business e le regole di dominio:

- **User**: Rappresenta un giocatore di padel
  - Attributi: username, email, livello dichiarato, livello percepito
  - Business logic: calcolo statistiche partite

- **Match**: Rappresenta una partita di padel
  - Attributi: location, dateTime, livello richiesto, tipo, stato
  - Business logic: controllo posti disponibili, calcolo iscritti attivi

- **Registration**: Rappresenta l'iscrizione di un utente a una partita
  - Gestisce la relazione many-to-many tra User e Match

- **Feedback**: Rappresenta la valutazione post-partita
  - Vincolo: un feedback per coppia utente-partita

#### View (Presentazione)
**Package**: `src/main/resources/templates`

Template Thymeleaf per renderizzazione HTML:

- `index.html`: Homepage con statistiche e CTA
- `matches.html`: Lista partite con filtri
- `users.html`: Community giocatori
- `create-match.html`: Form creazione partita

**Caratteristiche**:
- Server-side rendering
- Binding bidirezionale con model
- Supporto i18n (preparato per internazionalizzazione)

#### Controller (Controllo)
**Package**: `com.example.padel_app.controller`

**Web Controller** (`WebController`):
- Gestisce tutte le richieste HTTP dell'applicazione
- Rendering pagine HTML con Thymeleaf
- Gestione form submission (POST requests)
- Redirect e flash attributes per messaggi utente
- Orchestrazione tra Service layer e View layer

---

### 2.2 Repository Pattern

**Package**: `com.example.padel_app.repository`

Astrae l'accesso ai dati dal resto dell'applicazione.

```java
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStatus(MatchStatus status);
    
    @Query("SELECT DISTINCT m FROM Match m 
            LEFT JOIN FETCH m.creator 
            LEFT JOIN FETCH m.registrations")
    List<Match> findAllWithCreator();
}
```

**Vantaggi**:
- Isolamento logica persistenza
- Query personalizzate con @Query
- JOIN FETCH per ottimizzazione lazy loading
- Testing facilitato con mock

---

### 2.3 Service Layer Pattern

**Package**: `com.example.padel_app.service`

Incapsula la logica business e orchestra le operazioni.

**MatchService**:
- Gestisce creazione, join, leave partite
- Pubblica eventi Observer
- Utilizza Strategy pattern per ordinamento

**RegistrationService**:
- Gestisce iscrizioni con validazioni
- Controlla vincoli business (max 4 giocatori)

**FeedbackService**:
- Gestisce feedback post-partita
- Calcola e aggiorna livello percepito

**NotificationService** (Singleton):
- Servizio centralizzato per notifiche
- Log eventi di sistema

---

## 3. Design Patterns

### 3.1 Observer Pattern

**Problema**: Notificare componenti interessati quando lo stato di una partita cambia.

**Soluzione**: Pattern Observer con eventi Spring

#### Implementazione

**Eventi** (`com.example.padel_app.event`):
```java
public class MatchConfirmedEvent extends ApplicationEvent {
    private final Match match;
    private final LocalDateTime timestamp;
}

public class MatchFinishedEvent extends ApplicationEvent {
    private final Match match;
    private final LocalDateTime timestamp;
}
```

**Publisher** (`MatchService`):
```java
@Service
public class MatchService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void confirmMatch(Match match) {
        match.setStatus(MatchStatus.CONFIRMED);
        matchRepository.save(match);
        
        // Pubblica evento
        eventPublisher.publishEvent(
            new MatchConfirmedEvent(this, match)
        );
    }
}
```

**Listener** (`MatchEventListener`):
```java
@Component
public class MatchEventListener {
    @EventListener
    public void handleMatchConfirmed(MatchConfirmedEvent event) {
        notificationService.sendMatchConfirmedNotification(
            event.getMatch()
        );
    }
}
```

**Vantaggi**:
- Disaccoppiamento tra publisher e subscriber
- Facile aggiunta di nuovi listener
- Spring gestisce lifecycle e threading

---

### 3.2 Strategy Pattern

**Problema**: Implementare diversi algoritmi di ordinamento partite in modo intercambiabile.

**Soluzione**: Pattern Strategy con interfaccia e implementazioni concrete

#### Implementazione

**Interfaccia Strategia**:
```java
public interface MatchSortingStrategy {
    List<Match> sort(List<Match> matches);
}
```

**Strategie Concrete**:

```java
@Component("dateSorting")
public class DateSortingStrategy implements MatchSortingStrategy {
    @Override
    public List<Match> sort(List<Match> matches) {
        return matches.stream()
            .sorted(Comparator.comparing(Match::getDateTime))
            .collect(Collectors.toList());
    }
}

@Component("popularitySorting")
public class PopularitySortingStrategy implements MatchSortingStrategy {
    @Override
    public List<Match> sort(List<Match> matches) {
        return matches.stream()
            .sorted(Comparator.comparing(
                Match::getActiveRegistrationsCount
            ).reversed())
            .collect(Collectors.toList());
    }
}
```

**Context** (`MatchService`):
```java
@Service
public class MatchService {
    @Autowired
    private Map<String, MatchSortingStrategy> strategies;
    
    public List<Match> getMatchesSorted(String sortType) {
        MatchSortingStrategy strategy = strategies.get(sortType);
        return strategy.sort(matches);
    }
}
```

**Vantaggi**:
- Facile aggiunta di nuovi algoritmi
- Selezione runtime della strategia
- Codice più manutenibile e testabile

---

### 3.3 Singleton Pattern

**Problema**: Garantire una singola istanza del servizio notifiche.

**Soluzione**: Spring bean singleton scope (default)

#### Implementazione

```java
@Service
@Scope("singleton") // Esplicito per documentazione
public class NotificationService {
    
    public void sendMatchConfirmedNotification(Match match) {
        log.info("🎉 Partita confermata: {} - 4 giocatori!", 
                 match.getLocation());
    }
    
    public void sendMatchFinishedNotification(Match match) {
        log.info("🏁 Partita terminata: {}", match.getLocation());
    }
}
```

**Caratteristiche**:
- Spring garantisce singleton per default
- @Scope("singleton") rende esplicita l'intenzione
- Thread-safe grazie al container Spring
- Lazy initialization controllata da Spring

---

## 4. Decisioni di Design

### 4.1 Gestione Lazy Loading

**Problema**: LazyInitializationException quando si accede a relazioni lazy fuori dalla sessione Hibernate.

**Decisione**: Utilizzo di JOIN FETCH nelle query

```java
@Query("SELECT DISTINCT m FROM Match m 
        LEFT JOIN FETCH m.creator 
        LEFT JOIN FETCH m.registrations")
List<Match> findAllWithCreator();
```

**Motivazione**:
- Evita N+1 query problem
- Caricamento eager controllato
- Prestazioni migliori rispetto a @Transactional su view

### 4.2 Validazione Vincoli Business

**Vincolo**: Massimo 4 giocatori per partita

**Implementazione**: Validazione a livello service

```java
public Registration joinMatch(Long matchId, Long userId) {
    Match match = matchRepository.findById(matchId)
        .orElseThrow(() -> new MatchNotFoundException());
    
    if (match.isFull()) {
        throw new MatchFullException(
            "La partita ha raggiunto il numero massimo di giocatori"
        );
    }
    
    // Logica iscrizione...
}
```

**Vincolo**: Un feedback per utente per partita

**Implementazione**: Unique constraint database + validazione service

```java
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {
        "author_id", "target_user_id", "match_id"
    })
})
public class Feedback { ... }
```

### 4.3 Conferma Automatica Partite

**Requisito**: Partita confermata automaticamente al 4° giocatore

**Implementazione**:
1. Dopo ogni iscrizione, controllo numero giocatori
2. Se 4 → cambio stato a CONFIRMED
3. Pubblicazione evento MatchConfirmedEvent
4. Listener gestisce notifiche

```java
if (match.getActiveRegistrationsCount() >= 4) {
    match.setStatus(MatchStatus.CONFIRMED);
    matchRepository.save(match);
    
    eventPublisher.publishEvent(
        new MatchConfirmedEvent(this, match)
    );
}
```

### 4.4 Calcolo Livello Percepito

**Requisito**: Aggiornare livello percepito basato su feedback ricevuti

**Implementazione**: Media aritmetica dei feedback

```java
public void updatePerceivedLevel(Long userId) {
    List<Feedback> feedbacks = feedbackRepository
        .findByTargetUser(userRepository.findById(userId).get());
    
    if (!feedbacks.isEmpty()) {
        double avgLevel = feedbacks.stream()
            .mapToInt(f -> f.getSuggestedLevel().ordinal())
            .average()
            .orElse(0);
        
        Level perceivedLevel = Level.values()[(int) Math.round(avgLevel)];
        
        user.setPerceivedLevel(perceivedLevel);
    }
}
```

---

## 5. Gestione Dati e Persistenza

### 5.1 Schema Database

**Entità Principali**:
- `users`: Anagrafica giocatori
- `matches`: Partite di padel
- `registrations`: Iscrizioni partite (relazione N:M)
- `feedbacks`: Valutazioni post-partita

**Relazioni**:
- User 1:N Match (creatore)
- User N:M Match (tramite Registration)
- User 1:N Feedback (autore)
- User 1:N Feedback (destinatario)
- Match 1:N Registration
- Match 1:N Feedback

### 5.2 Ottimizzazioni Query

**Problematica**: N+1 queries

**Soluzione**:
```java
// Invece di:
List<Match> matches = matchRepository.findAll();
// Che genera N query per caricare creator

// Usiamo:
List<Match> matches = matchRepository.findAllWithCreator();
// Una query con JOIN FETCH
```

**Risultato**: Da N+1 query a 1 query

### 5.3 Transazionalità

**Servizi Transazionali**:
```java
@Service
@Transactional
public class MatchService {
    // Metodi CRUD automaticamente transazionali
}
```

**Propagation**:
- `REQUIRED`: Default, usa transazione esistente o ne crea una
- `REQUIRES_NEW`: Per operazioni indipendenti (es. log audit)

---

## Conclusioni

L'architettura implementata garantisce:

✅ **Manutenibilità**: Codice ben organizzato e modulare  
✅ **Scalabilità**: Facile aggiunta di nuove funzionalità  
✅ **Testabilità**: Dipendenze iniettate, facili da mockare  
✅ **Performance**: Query ottimizzate, lazy loading controllato  
✅ **Best Practices**: Pattern consolidati, principi SOLID  

La struttura è pronta per evoluzioni future come:
- Autenticazione JWT
- API GraphQL
- Microservizi
- Mobile app integration
