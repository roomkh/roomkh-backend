# docs/architecture.md

# Architecture

## System Architecture

React Frontend 

↓ HTTP / JSON

Spring Boot Backend

↓ JPA / Hibernate

PostgreSQL Database



The React frontend communicates with the Spring Boot backend exclusively over HTTP using JSON request and response bodies. The backend persists and retrieves data through Spring Data JPA and Hibernate, which translate Java entity operations into SQL statements executed against PostgreSQL.

## Supporting Infrastructure

| Component | Role |
|---|---|
| Docker Compose | Runs a local PostgreSQL container so the database environment is identical across machines without a native install |
| Flyway | Applies versioned SQL migrations to keep the database schema consistent and auditable across environments |
| Spring Security | Enforces stateless request authentication and role-based authorization on protected endpoints |
| JWT Access Token | A short-lived, signed token proving the caller's identity on each API request |
| Refresh Token Cookie | A long-lived, HttpOnly cookie used to silently obtain a new access token without requiring the user to log in again |

## Package Structure

| Package | Responsibility |
|---|---|
| `config` | Application-wide configuration beans: security, CORS, cookies, JWT properties, refresh token properties |
| `controller` | REST controllers that receive HTTP requests and return `ApiResponse` bodies |
| `dto` | Request and response data transfer objects; entities are never exposed directly through the API |
| `entity` | JPA entities and enums mapped to database tables |
| `exception` | Custom exceptions and the global exception handler |
| `mapper` | Converts entities to response DTOs |
| `repository` | Spring Data JPA repositories for database access |
| `security` | JWT generation/validation, authentication filter, user details services, cookie utilities |
| `service` | Business logic, orchestrating repositories, mappers, and security components |