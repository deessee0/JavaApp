# Padel App - Progetto Universitario

## Overview
This Spring Boot Java application manages padel matches, designed for a university project. It implements the MVC architecture, incorporates Observer, Strategy, and Singleton design patterns, and provides a streamlined web interface for quick demonstrations. The project aims to deliver a fully functional application ready for academic submission, showcasing key software engineering principles.

## Deployment & Avvio Rapido

### Script Automatici Multi-Piattaforma ✅
Il progetto include script **Linux/Mac (.sh) e Windows (.bat)** per semplificare avvio e testing:

**Avvio Locale (richiede Java 17+):**
- **Linux/Mac:** `./scripts/run-local.sh`
- **Windows:** `scripts\run-local.bat`
- Include controlli prerequisiti (Java version check)
- Porta 5000: http://localhost:5000
- Login demo: margherita.biffi@padel.it / password123

**Test Automatici (81 test):**
- **Linux/Mac:** `./scripts/run-tests.sh`
- **Windows:** `scripts\run-tests.bat`
- Esegue test suite completa
- Genera report JaCoCo coverage in `target/site/jacoco/index.html`
- **Tutti gli 81 test passano** ✅

**Docker Production-Ready:**
- **Linux/Mac:** `./scripts/run-docker.sh`
- **Windows:** `scripts\run-docker.bat`
- Build multi-stage (JDK → JRE Alpine, ~150MB)
- Supporta sia `docker compose` (nuovo) che `docker-compose` (vecchio)
- Health check automatico (~40s per startup completo)

**✨ Robustezza Script (Nov 2025):**
- Tutti gli script cambiano automaticamente directory alla root del progetto
- Funzionano indipendentemente da dove vengono eseguiti
- Windows: `cd /d "%~dp0\.."` | Linux/Mac: `cd "$(dirname "$0")/.."`
- Fix per errore "mvnw.cmd not found" su Windows

**Guida completa:** Vedi `scripts/README.md` per troubleshooting e dettagli

### Docker Production-Ready
- **Dockerfile**: Multi-stage build con Java 17 JDK → JRE Alpine (~150MB finale)
- **docker-compose.yml**: Configurazione porta 5000, health checks, restart policy
- **.dockerignore**: Build ottimizzato (esclude target/, .git/, docs/)

### QUICKSTART.md
Guida rapida per professori/revisori con:
- Avvio in 30 secondi
- Funzionalità da testare durante demo
- Troubleshooting comuni
- Checklist pre-consegna

## User Preferences
I prefer the agent to act as a mentor, guiding me through the development process.
I want to use Java 17 and Spring Boot 3.5.5.
I prefer detailed explanations for complex concepts and design pattern implementations.
I expect the agent to prioritize fixing `LazyInitializationException` and ensuring robust data handling.
I want the agent to use a test-driven approach, focusing on scenario-based tests and improving code coverage.
I need assistance in maintaining comprehensive documentation, including UML diagrams and clear comments, especially for educational purposes.
I prefer that the agent focuses on the core business logic and architectural integrity, rather than extensive UI/UX refinements beyond the current simplified design.
I want to ensure all implemented design patterns are correctly applied and documented.

## System Architecture
The application uses **Spring Boot 3.5.5** with **Java 17** and **Maven**. It follows the **MVC architecture**.

**UI/UX Decisions:**
The UI is designed for rapid demonstration, featuring a minimalist homepage, reduced navigation, clean design with neutral colors, a single filter dropdown, and immediate action buttons.

**Technical Implementations:**
-   **Embedded Tomcat** on port 5000.
-   **Thymeleaf** for templating.
-   **Lombok** for boilerplate reduction.
-   **Validation** for input handling.
-   **H2 in-memory database** for development.
-   **Spring Boot DevTools** for hot reloading.
-   **Spring Actuator** for monitoring.
-   **Session-based Authentication**: HTTP session management with `UserSessionService` for login/register functionality. All endpoints require authentication.
-   **Demo Credentials**: Margherita Biffi (margherita.biffi@padel.it / password123) is the default demo user.
-   **POST-Redirect-GET pattern** is used in web controllers.

**Feature Specifications:**
-   **Authentication System**: Login/register with email and password. Session-based auth with redirect to /login for unauthenticated access. **✅ BCrypt password hashing** implementato per sicurezza (Nov 2025).
-   **User Management**: Registration with declared and perceived skill levels.
-   **Match Management**: Create, join, leave matches; automatic confirmation when 4 players join. Match deletion if the creator leaves. Only creator can finish matches. **Bug fix (Nov 2025)**: Riutilizzo registrations CANCELLED per evitare unique constraint violation su ri-iscrizione.
-   **Feedback System**: Players rate others post-match, updating perceived skill levels.
-   **Dynamic Sorting**: Matches can be sorted by date, popularity, or required level using the Strategy Pattern.
-   **Notifications**: Implemented via the Observer Pattern for match confirmations and completions.
-   **Data Seeding**: Automatic seeding of a complete dataset (users, matches, registrations) for testing. All users created with password "password123" (auto-upgrade a BCrypt al primo login).

**System Design Choices:**
-   **Observer Pattern**: `MatchConfirmedEvent` and `MatchFinishedEvent` are published by `ApplicationEventPublisher` and handled by `MatchEventListener` for notifications.
-   **Strategy Pattern**: `MatchSortingStrategy` interface with concrete implementations (`DateSortingStrategy`, `PopularitySortingStrategy`, `LevelSortingStrategy`) dynamically selected by `MatchService`.
-   **Singleton Pattern**: Leveraged through Spring's IoC container; all service beans and `ApplicationEventPublisher` are singletons by default.
-   **JPA Entities**: `User`, `Match`, `Registration` (association class), `Feedback` with appropriate relationships (`@OneToMany`, `@ManyToOne`, `JOIN FETCH` for optimizing lazy loading).
-   **Transactional Management**: `@Transactional` annotation ensures ACID properties and automatic rollbacks for service operations and data seeding.
-   **Testing**: JUnit 5, Spring Boot Test, and JaCoCo for coverage analysis, focusing on scenario-based integration tests.

## External Dependencies
-   **Spring Boot 3.5.5**: Core framework.
-   **Maven**: Build automation tool.
-   **H2 Database**: In-memory database for development.
-   **Thymeleaf**: Server-side Java template engine.
-   **Lombok**: Annotation processor for reducing boilerplate code.
-   **Spring Data JPA**: For data access and persistence.
-   **Spring Web**: For building web applications.
-   **Spring Boot Actuator**: For monitoring and managing the application.
-   **Spring Security Crypto**: BCrypt password hashing (security best practice).
-   **JaCoCo**: For code coverage reporting.
## Diagrammi UML

### Class Diagram Completo
File: `docs/class-diagram.puml`

Diagramma delle classi completo che mostra:
- Tutte le entità del dominio (User, Match, Registration, Feedback)
- Tutti i service e controller layers
- **Tutti i 3 design pattern** evidenziati:
  - **Observer Pattern**: Eventi + Listener
  - **Strategy Pattern**: Interfaccia + 3 implementazioni concrete
  - **Singleton Pattern**: NotificationService
- Relazioni JPA (@OneToMany, @ManyToOne)
- Package organization completa

### Use Case Diagram
File: `docs/use-case-diagram.puml`

Diagramma dei casi d'uso completo:
- Attori: Giocatore e Sistema
- Use cases autenticazione: Login (UC0), Logout (UC15), Registrazione Utente (UC1)
- Use cases business: Crea/Iscriviti/Lascia Match, Feedback, Analytics
- **Aggiornamento Nov 2025**: Rimossi `<<requires auth>>` dependencies errati che implicavano login per-request
- Nota esplicativa: descrive session-based auth con redirect automatico a /login
- WebController verifica getCurrentUser(session) su tutti gli endpoint protetti

### Sequence Diagrams

**Observer Pattern**: `docs/observer-sequence-diagram.puml`
- Flusso completo pubblicazione evento MatchConfirmedEvent
- Interazione Publisher → Event → Listener → NotificationService
- Dimostra disaccoppiamento tra componenti

**Strategy Pattern**: `docs/strategy-sequence-diagram.puml`
- Selezione runtime della strategia di sorting
- 3 scenari: ordinamento per data, popolarità, livello
- Mostra Spring Dependency Injection delle strategie

## Testing e Coverage

### Test Suite Completa (81 test totali) ✅

#### CompleteServiceTest (14 test - Integration Test)
Test scenario-based per business logic completa con Spring context:
- testCreateMatch, testJoinMatch, testAutoConfirmAt4Players ⭐
- testMaxPlayersLimit, testDuplicateRegistrationConstraint, testLeaveMatch
- testCreateFeedback, testOneFeedbackPerUserPerMatch, testPerceivedLevelUpdate
- testStrategyDateSorting, testStrategyPopularitySorting, testStrategyLevelSorting
- testFilterByStatus, testFilterByLevel

#### StrategyPatternTest (6 test - Unit Test)
Test isolato per Strategy Pattern (no Spring context):
- testDateSorting_AscendingOrder, testPopularitySorting_DescendingOrder
- testLevelSorting_AscendingOrder, testStrategyInterchangeability
- testEmptyList, testSingleMatch

#### MatchBusinessLogicTest (7 test) ⭐ NUOVO
Test regole business critiche:
- testCreatorLeavingMatch_DeletesEntireMatch (creator lascia → match eliminato)
- testNormalPlayerLeavingMatch_OnlyCancelsRegistration
- testCannotLeaveMatch_IfNotRegistered, testCannotLeaveMatch_IfAlreadyLeft
- testFinishMatch_ChangesStatusToFinished, testCannotFinishMatch_IfNotConfirmed
- testPlayerCountAfterLeave_DocumentsBehavior

#### ObserverPatternTest (5 test) ⭐ NUOVO
Test Observer Pattern con @RecordApplicationEvents:
- testMatchConfirmedEventPublishedWhen4PlayersJoin (verifica evento a 4 giocatori)
- testMatchFinishedEventPublishedWhenMatchFinished
- testNoEventPublishedIfLessThan4Players, testFinishMatchPublishesEvent
- testMatchConfirmedEventContainsCorrectData

#### FeedbackValidationTest (7 test) ⭐ NUOVO
Test validation rules sistema feedback:
- testCreateFeedback_BetweenPlayersOfSameMatch
- testCannotGiveFeedbackToSelf (documenta comportamento)
- testCannotGiveDuplicateFeedback (vincolo unicità DB)
- testPerceivedLevelCalculation (algoritmo media livello)
- testPerceivedLevel_NullWithoutFeedback
- testFeedback_OnlyOnFinishedMatches
- testFullFeedbackRound_AllPlayersToAllOthers

#### MatchServiceEdgeCasesTest (12 test) ⭐ NUOVO
Test edge cases per aumentare branch coverage:
- testStrategyFallback_WhenStrategyNotFound (fallback quando strategia non esiste)
- testSortingStrategy_WithEmptyList
- testCheckAndConfirmMatch_AlreadyConfirmed
- testCheckAndConfirmMatch_LessThan4Players
- testFinishMatch_MatchNotFound, testFinishMatch_NotConfirmed
- testFinishMatch_AlreadyFinished, testFinishMatch_Success
- testGetMatchesByStatus_NoMatches, testGetMatchesByLevel_FilterCorrectly
- testSaveMatch_UpdateExisting, testDeleteMatch_RemovesFromDatabase

#### NotificationServiceTest (7 test) ⭐ NUOVO
Test Singleton Pattern e NotificationService:
- testSendMatchConfirmedNotification_LogsCorrectMessage
- testSendMatchConfirmedNotification_DifferentMatches
- testSendMatchFinishedNotification_LogsCorrectMessage
- testSendMatchFinishedNotification_DifferentMatches
- testNotifications_HandleNullLocation
- testNotifications_MultipleCallsSafe
- testSingletonPattern_SharedState

#### AuthControllerTest (10 test) ⭐ NUOVO (Nov 2025)
Test sistema autenticazione con BCrypt:
- testLoginWithValidCredentials, testLoginWithNonExistentEmail, testLoginWithWrongPassword
- testRegisterWithValidData, testRegisterWithDuplicateEmail, testRegisterWithDuplicateUsername
- testLogout, testLogoutWhenNotAuthenticated
- testSessionPersistenceBetweenRequests, testIndependentSessions

#### MatchFilterTest (12 test) ⭐ NUOVO (Nov 2025)
Test filtri ricerca partite e Strategy Pattern:
- testFilterByStatus_WAITING, testFilterByStatus_CONFIRMED, testFilterByStatus_FINISHED
- testFilterByLevel_PRINCIPIANTE, testFilterByLevel_INTERMEDIO, testFilterByLevel_AVANZATO
- testCombinationFilter_StatusAndLevel, testCombinationFilter_OnlyStatusProvided
- testSortingStrategy_DateAscending, testSortingStrategy_PopularityDescending
- testSortingStrategy_LevelAscending, testFilterAndSort_Combined

#### PadelAppApplicationTests (1 test)
- Context load test (verifica startup Spring)

### Coverage JaCoCo Report (Aggiornato - 81 test)
Generato automaticamente dopo `mvnw test`:
- **Instruction Coverage**: ~55% 
- **Line Coverage**: ~54%  
- **Branch Coverage**: ~30%

**Nuovi test aggiunti (Nov 2025):**
- AuthControllerTest: 10 test per autenticazione e BCrypt
- MatchFilterTest: 12 test per filtri e Strategy Pattern
- MatchServiceEdgeCasesTest: 12 test per edge cases e branch coverage
- NotificationServiceTest: 7 test per Singleton Pattern

**Aree NON coperte (design choice):**
- WebController (173 lines) - focus su business logic, non controller layer
- UserService (18 lines) - CRUD semplice, bassa priorità
- UserContext (2 lines) - utility class minimale

**Aree BEN coperte:**
- MatchService, RegistrationService, FeedbackService (core business logic)
- Strategy Pattern implementations (100% copertura)
- Observer Pattern (eventi e listener)
- Model entities e validation rules

### Approccio Testing Adottato
- **Integration Tests** con @SpringBootTest per testare l'intera applicazione
- **@Transactional** su test per rollback automatico (DB pulito)
- **Scenario-based testing** (GIVEN-WHEN-THEN pattern)
- **Focus su business rules critiche** invece di coverage percentuale alta
- **Commenti didattici completi** in italiano per scopo universitario

