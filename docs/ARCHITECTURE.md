# Architettura Software - App Padel

## Indice
1. [Panoramica Architetturale](#panoramica-architetturale)
2. [Pattern Architetturali](#pattern-architetturali)
3. [Design Patterns](#design-patterns)
4. [Decisioni di Design](#decisioni-di-design)
5. [Gestione Dati e Persistenza](#gestione-dati-e-persistenza)

---

## 1. Panoramica Architetturale

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
- JaCoCo (code coverage)

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

Due tipi di controller:

**REST Controllers** (`MatchController`):
- API RESTful per operazioni CRUD
- Gestione richieste JSON
- Endpoint per frontend SPA (futuro)

**Web Controllers** (`WebController`):
- Rendering pagine HTML
- Gestione form submission
- Redirect e model attributes

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
- Aggiorna direttamente lo stato e registra i passaggi significativi nei log
- Utilizza Strategy pattern per ordinamento

**RegistrationService**:
- Gestisce iscrizioni con validazioni
- Controlla vincoli business (max 4 giocatori)

**FeedbackService**:
- Gestisce feedback post-partita
- Calcola e aggiorna livello percepito

---

## 3. Design Patterns

### 3.1 Strategy Pattern

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
@Component("date")
public class DateSortingStrategy implements MatchSortingStrategy {
    @Override
    public List<Match> sort(List<Match> matches) {
        return matches.stream()
            .sorted(Comparator.comparing(Match::getDateTime))
            .collect(Collectors.toList());
    }
}

@Component("popularity")
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

### 3.2 Gestione dello stato delle partite

**Problema**: Coordinare la transizione di stato delle partite quando si verifica una condizione business (4 giocatori registrati o partita scaduta).

**Soluzione**: Logica incapsulata in `MatchService`, che aggiorna lo stato direttamente tramite repository e registra l'esito nei log.

**Caratteristiche**:
- Riduce la complessità eliminando dipendenze tra publisher e listener
- Facilita il debugging grazie ai log espliciti delle transizioni di stato
- Mantiene il focus del servizio sulla logica business principale

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
1. Dopo ogni iscrizione, il servizio recupera il numero di giocatori attivi dal repository
2. Se raggiunge 4 → cambio stato a `CONFIRMED`
3. Salvataggio della partita e log del cambio stato

```java
if (activeCount >= 4 && match.getStatus() == MatchStatus.WAITING) {
    match.setStatus(MatchStatus.CONFIRMED);
    Match savedMatch = matchRepository.save(match);
    log.info("Match {} status changed from {} to {}",
             savedMatch.getId(), MatchStatus.WAITING, MatchStatus.CONFIRMED);
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
