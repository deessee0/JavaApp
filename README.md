# App Padel - Sistema di Gestione Partite

## 📋 Informazioni Progetto

**Corso**: Ingegneria del Software  
**Anno Accademico**: 2024/2025  
**Università**: [Nome Università]  
**Studente**: [Nome Cognome - Matricola]  

**Repository**: [Link GitHub]  
**Demo Live**: [ deployment]

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

## 🔒 Sicurezza e Autenticazione

### Sistema di Autenticazione

L'applicazione implementa un sistema di **autenticazione session-based** con gestione sicura delle password:

#### BCrypt Password Hashing ✅

**Scopo**: Proteggere le password degli utenti con hashing sicuro

**Implementazione**:
- `SecurityConfig`: Configura il bean BCryptPasswordEncoder
- `AuthController`: Utilizza BCrypt per hash e verifica password
- **Auto-upgrade**: Password legacy in chiaro vengono automaticamente convertite a BCrypt al primo login

**Caratteristiche**:
- **Algoritmo BCrypt**: Hashing sicuro con salt automatico
- **Protezione brute-force**: Iterazioni configurabili (default 10 rounds)
- **Retro-compatibilità**: Supporto password legacy con conversione automatica
- **Varianti supportate**: $2a$, $2b$, $2y$ (tutti i formati BCrypt standard)

**File coinvolti**:
- `src/main/java/com/example/padel_app/config/SecurityConfig.java`
- `src/main/java/com/example/padel_app/controller/AuthController.java`

#### Flusso di Registrazione

```java
1. User inserisce email/password nel form
2. AuthController.register() riceve credenziali in chiaro
3. Password viene hashata con BCrypt: 
   String hashedPassword = passwordEncoder.encode(plainPassword)
4. Hash salvato nel database (NON la password originale)
5. User creato con password protetta
```

#### Flusso di Login

```java
1. User inserisce email/password
2. AuthController.login() recupera user dal database
3. Verifica password:
   - Se hash BCrypt → passwordEncoder.matches(plain, hash)
   - Se plaintext (legacy) → conversione automatica a BCrypt
4. Se match → salva user in sessione HTTP
5. Redirect a homepage autenticata
```

#### Auto-Upgrade Password Legacy

Per garantire compatibilità con dati esistenti, il sistema supporta:

```java
// Login check con auto-upgrade
if (storedPassword.startsWith("$2a$") || 
    storedPassword.startsWith("$2b$") || 
    storedPassword.startsWith("$2y$")) {
    // Password già hashata → verifica BCrypt
    passwordMatches = passwordEncoder.matches(password, storedPassword);
} else {
    // Password legacy in chiaro → verifica e upgrade
    passwordMatches = password.equals(storedPassword);
    if (passwordMatches) {
        // AUTO-UPGRADE: converte a BCrypt
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }
}
```

### Gestione Sessioni HTTP

**Scopo**: Mantenere stato autenticazione tra richieste HTTP

**Implementazione**:
- `UserSessionService`: Gestisce salvataggio/recupero user da sessione
- **Session invalidation**: Logout completo con `session.invalidate()`
- **Security checks**: Ogni endpoint protetto verifica presenza user in sessione

**Pattern**: 
- POST-Redirect-GET per prevenire double-submit
- Flash messages per feedback utente



## 📦 Struttura del Progetto

```
src/main/java/com/example/padel_app/
├── config/                    # Configurazioni e inizializzazione
│   ├── SecurityConfig.java   # Configurazione BCrypt per password hashing
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

## 👨‍💻 Autore

**[Nome Cognome]**  
Matricola: [Numero Matricola]  
Email: [email@university.it]  

## 📄 Licenza

Progetto sviluppato per scopi didattici - Corso di Ingegneria del Software
