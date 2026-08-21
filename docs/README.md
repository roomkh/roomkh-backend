# docs/README.md

# RoomKH Backend Documentation

RoomKH Backend is the Spring Boot REST API powering the RoomKH property marketplace platform for Cambodia. This documentation set covers everything implemented so far: project setup, the user/role foundation, standard API conventions, and the full authentication system (registration, JWT login, refresh tokens, and email-or-phone identifier support).

## Table of Contents

### Overview
- [Project Overview](./project-overview.md)
- [Architecture](./architecture.md)

### Database
- [Database Schema](./database/schema.md)
- [Flyway Migrations](./database/flyway-migrations.md)

### API Reference
- [Authentication API](./api/authentication.md)

### Security
- [JWT Authentication](./security/jwt-authentication.md)
- [Refresh Token Strategy](./security/refresh-token.md)

### Feature Records
- [01 - Project Setup](./features/01-project-setup.md)
- [02 - User and Role Foundation](./features/02-user-role-foundation.md)
- [03 - API Response and Exception Handling](./features/03-api-response-exception.md)
- [04 - User Registration](./features/04-user-registration.md)
- [05 - JWT Authentication](./features/05-jwt-authentication.md)
- [06 - Refresh Token, Remember Me, and Logout](./features/06-refresh-token-remember-me.md)
- [07 - Email or Phone Authentication Policy Alignment](./features/07-auth-policy-alignment.md)