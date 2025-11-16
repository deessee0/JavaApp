# Padel App - Progetto Universitario

## Overview
This Spring Boot Java application manages padel matches, designed for a university project. It implements the MVC architecture, incorporates Observer, Strategy, and Singleton design patterns, and provides a streamlined web interface for quick demonstrations. The project aims to deliver a fully functional application ready for academic submission, showcasing key software engineering principles.

## Deployment & Avvio Rapido

### Script Automatici (Nuovo)
Il progetto include script bash per semplificare avvio e testing:
- **`scripts/run-local.sh`**: Avvio rapido con Maven (include controlli prerequisiti)
- **`scripts/run-tests.sh`**: Esegue test suite + genera report JaCoCo
- **`scripts/run-docker.sh`**: Avvio con Docker Compose

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
-   **UserContext** is used to simulate a single logged-in user (Margherita Biffi) for simplified demonstration.
-   **POST-Redirect-GET pattern** is used in web controllers.

**Feature Specifications:**
-   **User Management**: Registration with declared and perceived skill levels.
-   **Match Management**: Create, join, leave matches; automatic confirmation when 4 players join. Match deletion if the creator leaves.
-   **Feedback System**: Players rate others post-match, updating perceived skill levels.
-   **Dynamic Sorting**: Matches can be sorted by date, popularity, or required level using the Strategy Pattern.
-   **Notifications**: Implemented via the Observer Pattern for match confirmations and completions.
-   **Data Seeding**: Automatic seeding of a complete dataset (users, matches, registrations) for testing.

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

### Test Suite Completa (59 test totali) ✅

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

#### PadelAppApplicationTests (1 test)
- Context load test (verifica startup Spring)

### Coverage JaCoCo Report (Aggiornato - 59 test)
Generato automaticamente dopo `mvnw test`:
- **Instruction Coverage**: 54.8% (1405/2565 instructions)
- **Line Coverage**: 54.2% (295/544 lines)  
- **Branch Coverage**: 29.4% (25/85 branches)

**Nuovi test aggiunti:**
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

