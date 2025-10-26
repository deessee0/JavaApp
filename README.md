# App Padel - Sistema di Gestione Partite

## 📋 Informazioni Progetto

**Corso**: Ingegneria del Software  
**Anno Accademico**: 2024/2025  
**Università**: [Nome Università]  
**Studente**: [Nome Cognome - Matricola]  

**Repository**: [Link GitHub]  
**Demo Live**: [Link Replit o deployment]

## 📝 Descrizione del Progetto

### Ambito

L'applicazione **App Padel** è un sistema di gestione partite di padel tra giocatori sconosciuti che permette di:
- Creare partite proposte o fisse
- Gestire iscrizioni con limite massimo di 4 giocatori
- Confermare automaticamente le partite al raggiungimento del numero massimo
- Fornire feedback post-partita per valutare il livello degli altri giocatori

### Obiettivi

Il progetto dimostra l'applicazione pratica dei principi di Ingegneria del Software attraverso:
1. **Architettura MVC** ben strutturata
2. **Design Patterns** (Observer, Strategy, Singleton)
3. **Persistenza dati** con JPA/Hibernate
4. **Interfaccia utente** web con Thymeleaf
5. **Testing** con coverage ≥80%

## 🎯 Requisiti Funzionali

### RF1: Gestione Utenti
- **RF1.1**: Registrazione utente con livello dichiarato (Principiante, Intermedio, Avanzato, Professionista)
- **RF1.2**: Visualizzazione profilo utente con statistiche partite
- **RF1.3**: Calcolo livello percepito basato su feedback ricevuti

### RF2: Gestione Partite
- **RF2.1**: Creazione partita (fissa o proposta)
- **RF2.2**: Join partita con controllo massimo 4 giocatori
- **RF2.3**: Leave partita (solo se non confermata)
- **RF2.4**: Conferma automatica al raggiungimento di 4 iscritti
- **RF2.5**: Filtro e ordinamento partite (per data, popolarità, livello)

### RF3: Sistema Feedback
- **RF3.1**: Inserimento feedback post-partita
- **RF3.2**: Vincolo: un feedback per utente per partita
- **RF3.3**: Aggiornamento livello percepito basato su media feedback

### RF4: Notifiche
- **RF4.1**: Notifica conferma partita (4 giocatori)
- **RF4.2**: Notifica termine partita

## 🔧 Requisiti Non Funzionali

### RNF1: Architettura
- Pattern MVC con separazione Controller/Service/Repository
- Utilizzo di almeno 2 design pattern oltre a quelli del framework

### RNF2: Tecnologie
- **Backend**: Spring Boot 3.5.5, Java 17
- **Database**: H2 (development), JPA/Hibernate
- **Frontend**: Thymeleaf, HTML/CSS
- **Testing**: JUnit 5, JaCoCo (coverage ≥80%)

### RNF3: Qualità
- Codice ben documentato con Javadoc
- Test di unità per logica business
- Gestione corretta delle eccezioni

## 🏗️ Architettura del Sistema

### Pattern Architetturali

#### 1. MVC (Model-View-Controller)
- **Model**: Entità JPA (User, Match, Registration, Feedback)
- **View**: Template Thymeleaf (HTML)
- **Controller**: WebController per rendering pagine e gestione form

#### 2. Repository Pattern
- Interfacce estendono JpaRepository
- Query personalizzate con @Query
- Separazione accesso dati da logica business

### Design Patterns Implementati

#### 1. Observer Pattern
**Scopo**: Notificare gli interessati quando cambiano gli stati delle partite

**Implementazione**:
- `MatchConfirmedEvent`: Evento pubblicato quando una partita raggiunge 4 giocatori
- `MatchFinishedEvent`: Evento pubblicato quando una partita termina
- `MatchEventListener`: Listener che gestisce gli eventi
- `NotificationService`: Singleton che invia le notifiche

**File coinvolti**:
- `src/main/java/com/example/padel_app/event/MatchConfirmedEvent.java`
- `src/main/java/com/example/padel_app/event/MatchFinishedEvent.java`
- `src/main/java/com/example/padel_app/listener/MatchEventListener.java`

#### 2. Strategy Pattern
**Scopo**: Implementare diversi algoritmi di ordinamento partite in modo intercambiabile

**Implementazione**:
- `MatchSortingStrategy`: Interfaccia strategia
- `DateSortingStrategy`: Ordinamento per data
- `PopularitySortingStrategy`: Ordinamento per numero iscritti
- `LevelSortingStrategy`: Ordinamento per livello

**File coinvolti**:
- `src/main/java/com/example/padel_app/strategy/MatchSortingStrategy.java`
- `src/main/java/com/example/padel_app/strategy/DateSortingStrategy.java`
- `src/main/java/com/example/padel_app/strategy/PopularitySortingStrategy.java`
- `src/main/java/com/example/padel_app/strategy/LevelSortingStrategy.java`

#### 3. Singleton Pattern
**Scopo**: Garantire una singola istanza del servizio di notifiche

**Implementazione**:
- `NotificationService`: Annotato con @Service (singleton per default in Spring)
- `@Scope("singleton")`: Esplicita dichiarazione singleton

**File coinvolti**:
- `src/main/java/com/example/padel_app/service/NotificationService.java`

## 📦 Struttura del Progetto

```
src/main/java/com/example/padel_app/
├── config/                    # Configurazioni e inizializzazione
│   └── DataSeeder.java       # Popolamento DB con dati demo
├── controller/                # Controllers MVC
│   └── WebController.java    # Controller principale per tutte le pagine web
├── event/                     # Eventi Observer pattern
│   ├── MatchConfirmedEvent.java
│   └── MatchFinishedEvent.java
├── listener/                  # Listener Observer pattern
│   └── MatchEventListener.java
├── model/                     # Entità JPA
│   ├── User.java
│   ├── Match.java
│   ├── Registration.java
│   ├── Feedback.java
│   └── enums/                # Enumerazioni
│       ├── Level.java
│       ├── MatchType.java
│       ├── MatchStatus.java
│       └── RegistrationStatus.java
├── repository/                # Repository JPA
│   ├── UserRepository.java
│   ├── MatchRepository.java
│   ├── RegistrationRepository.java
│   └── FeedbackRepository.java
├── service/                   # Logica business
│   ├── MatchService.java
│   ├── RegistrationService.java
│   ├── FeedbackService.java
│   ├── UserService.java
│   └── NotificationService.java
├── strategy/                  # Strategy pattern
│   ├── MatchSortingStrategy.java
│   ├── DateSortingStrategy.java
│   ├── PopularitySortingStrategy.java
│   └── LevelSortingStrategy.java
└── PadelAppApplication.java  # Main class

src/main/resources/
├── templates/                 # Thymeleaf views
│   ├── index.html
│   ├── matches.html
│   ├── users.html
│   └── create-match.html
└── static/css/
    └── style.css

src/test/java/
└── com/example/padel_app/
    └── [Test classes]
```

## 🧪 Testing

### Strategia di Test

#### Test di Unità
- **MatchServiceTest**: Test logica gestione partite
- **RegistrationServiceTest**: Test iscrizioni e vincoli
- **FeedbackServiceTest**: Test feedback e livello percepito
- **StrategyPatternTest**: Test algoritmi ordinamento
- **ObserverPatternTest**: Test eventi e notifiche

#### Coverage
- Target: ≥80% coverage con JaCoCo
- Report generato in: `target/site/jacoco/index.html`

### Esecuzione Test

```bash
# Esegui tutti i test
./mvnw test

# Genera report coverage
./mvnw jacoco:report
```

## 🚀 Installazione e Avvio

### Prerequisiti
- Java 17 o superiore
- Maven 3.6+ (oppure usa Maven Wrapper incluso)

### Avvio Applicazione

```bash
# Clone repository
git clone [repository-url]
cd padel-app

# Avvia applicazione
./mvnw spring-boot:run
```

L'applicazione sarà disponibile su: `http://localhost:5000`

### Database H2 Console
- URL: `http://localhost:5000/h2-console`
- JDBC URL: `jdbc:h2:mem:padeldb`
- Username: `sa`
- Password: (vuota)

## 📊 Diagrammi UML

I diagrammi UML dettagliati sono disponibili nella cartella `/docs`:
- **Use Case Diagram**: `docs/use-case-diagram.puml`
- **Class Diagram**: `docs/class-diagram.puml`
- **Sequence Diagram**: `docs/sequence-diagram.puml`

## 🔄 Workflow Tipico

### 1. Creazione Partita
1. Utente accede a "Crea Partita"
2. Compila form (luogo, data, livello richiesto)
3. Sistema crea partita con stato WAITING
4. Creatore è automaticamente iscritto

### 2. Join Partita
1. Utente visualizza lista partite
2. Seleziona partita con posti disponibili
3. Click su "Unisciti"
4. Sistema registra iscrizione
5. Se 4° giocatore → Evento MatchConfirmed → Stato CONFIRMED

### 3. Feedback Post-Partita
1. Partita termina (stato FINISHED)
2. Giocatori accedono a pagina feedback
3. Valutano livello compagni
4. Sistema aggiorna livello percepito

## 📈 Funzionalità Future

- [ ] Sistema di autenticazione utenti
- [ ] Chat tra giocatori
- [ ] Sistema di ranking
- [ ] Notifiche push
- [ ] App mobile

## 👨‍💻 Autore

**[Nome Cognome]**  
Matricola: [Numero Matricola]  
Email: [email@university.it]  

## 📄 Licenza

Progetto sviluppato per scopi didattici - Corso di Ingegneria del Software
