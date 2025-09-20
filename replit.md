# Padel App

## Overview
This is a Spring Boot Java application for a padel management system. The application provides a web-based backend API with JPA database support.

## Project Architecture
- **Framework**: Spring Boot 3.5.5
- **Language**: Java 17
- **Build Tool**: Maven with Maven Wrapper (./mvnw)
- **Database**: H2 in-memory database (development)
- **Web Server**: Embedded Tomcat on port 5000
- **Dependencies**: Spring Web, Spring Data JPA, Spring Boot Actuator, H2 Database, Lombok, Validation

## Current Setup
- Configured for Replit environment with port 5000
- Server address set to 0.0.0.0 to work with Replit's proxy
- Forward headers strategy configured for iframe compatibility
- H2 console available at `/h2-console`
- Spring Boot DevTools enabled for hot reload
- Actuator endpoint available at `/actuator`

## Development Workflow
- **Workflow**: "Spring Boot Server" runs `./mvnw spring-boot:run`
- **Port**: 5000 (configured for frontend access)
- **Auto-reload**: Enabled via Spring Boot DevTools

## Deployment
- **Target**: Autoscale (stateless web application)
- **Command**: `./mvnw spring-boot:run`
- Suitable for REST API services

## Recent Changes
- 2025-09-20: Initial project import and setup for Replit environment
- Configured server properties for port 5000 and proxy compatibility
- Set up development workflow and deployment configuration

## Notes
- This is currently a basic Spring Boot starter with no custom endpoints
- Database is in-memory H2, data will reset on restart
- Ready for development of padel management features