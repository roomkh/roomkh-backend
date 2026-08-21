# Feature: Project Setup

## Status

Completed

## Purpose

Set up the initial RoomKH Spring Boot backend project with PostgreSQL,
Docker Compose, environment-based configuration, and Flyway database
migration support.

This setup provides the technical foundation for all future RoomKH backend
features, including authentication, seller management, property listings,
admin management, and security features.

## Technology Stack

- Java
- Spring Boot
- Maven
- PostgreSQL
- Docker Compose
- Flyway
- Spring Security
- IntelliJ IDEA
- Git and GitHub

## Project Structure

The backend uses the following base package structure:

```text
com.roomkh.backend
├── config
├── controller
├── dto
├── entity
├── exception
├── mapper
├── repository
├── security
├── service
└── RoomkhBackendApplication.java
```

## Configuration Files

The project uses separate configuration files for different environments.

```text
src/main/resources/
├── application.yml
├── application-dev.yml
└── application-prod.yml
```

### application.yml

Contains common configuration shared by all environments.

### application-dev.yml

Contains local development configuration.

Typical development configuration includes:

- Local PostgreSQL database connection
- Development server settings
- Flyway migration settings
- Development logging settings

### application-prod.yml

Contains production configuration template.

Production secrets must not be written directly in this file. Production
values must come from environment variables.

## PostgreSQL Database

The RoomKH backend uses PostgreSQL as the main relational database.

Initial database configuration:

```text
Database Name: roomkh_db
Username: roomkh_user
Password: loaded from environment variable
```

PostgreSQL is started locally using Docker Compose.

## Docker Compose

The project contains:

```text
docker-compose.yml
```

Docker Compose is used to start PostgreSQL locally without requiring a
manual PostgreSQL installation on the developer computer.

Typical command to start the database:

```bash
docker compose up -d
```

Typical command to stop the database:

```bash
docker compose down
```

## Environment Variables

Sensitive values must not be committed to GitHub.

The project contains:

```text
.env.example
```

This file provides safe environment variable examples without real secrets.

Example:

```env
POSTGRES_DB=roomkh_db
POSTGRES_USER=roomkh_user
POSTGRES_PASSWORD=change_me
JWT_SECRET=replace_with_a_long_secure_secret
JWT_EXPIRATION_MS=900000
```

The real local environment file is:

```text
.env
```

The `.env` file is ignored by Git and must never be pushed to GitHub.

## Flyway Database Migration

Flyway is used to manage PostgreSQL database schema changes.

Migration files are stored in:

```text
src/main/resources/db/migration/
```

Flyway runs database migrations automatically when Spring Boot starts.

Migration file naming format:

```text
V<version>__<description>.sql
```

Example:

```text
V1__create_roles_and_users.sql
V2__seed_default_roles.sql
```

## Important Flyway Rule

Once a Flyway migration has successfully run, it must not be edited.

Incorrect approach:

```text
Edit V1__create_roles_and_users.sql after it has run.
```

Correct approach:

```text
Create a new migration file.

Example:
V6__add_new_column_to_users.sql
```

## Git Ignore Rules

The project `.gitignore` prevents generated files, IDE files, and secrets
from being committed.

Important ignored files and folders:

```text
target/
.idea/
*.iml
.env
.env.*
```

The environment template remains tracked:

```text
.env.example
```

## Running the Application

Start PostgreSQL first:

```bash
docker compose up -d
```

Run the Spring Boot application:

```bash
./mvnw spring-boot:run
```

For Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend application runs on:

```text
http://localhost:8080
```

## Verification Checklist

- [x] Spring Boot project was created successfully.
- [x] Maven wrapper files were generated.
- [x] PostgreSQL Docker Compose configuration was created.
- [x] Local PostgreSQL container can start successfully.
- [x] application.yml configuration was created.
- [x] Development and production profiles were created.
- [x] Flyway was configured.
- [x] .env.example was created.
- [x] Real .env file is ignored by Git.
- [x] Spring Boot application can connect to PostgreSQL.
- [x] Project setup was committed and pushed to GitHub.

## Related Commit

```text
chore: initialize Spring Boot PostgreSQL Docker and Flyway
```