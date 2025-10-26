# Padel App - Progetto Universitario

## Overview
This Spring Boot Java application manages padel matches, designed for a university project. It implements the MVC architecture, incorporates Observer, Strategy, and Singleton design patterns, and provides a streamlined web interface for quick demonstrations. The project aims to deliver a fully functional application ready for academic submission, showcasing key software engineering principles.

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