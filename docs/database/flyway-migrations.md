# docs/database/flyway-migrations.md

# Flyway Migrations

## What Flyway Does

Flyway applies versioned SQL migration files to the database in strict numerical order and records which versions have already run in a `flyway_schema_history` table. This keeps every environment — a developer's laptop, a teammate's machine, or production — on an identical, auditable schema.

## Critical Rule

**Never edit a migration file after it has successfully run.** Once Flyway has applied a version, changing that file's contents creates a checksum mismatch that will cause Flyway to fail on the next run in any environment that already applied the original version. If a mistake needs fixing, create a new migration file instead of editing an old one.

## Migration History

| Version | File | Purpose |
|---|---|---|
| V1 | create roles and users | Create the `roles` and `users` tables |
| V2 | seed default roles | Seed the USER, SELLER, and ADMIN roles |
| V3 | unique phone number | Enforce a unique constraint on `phone_number` |
| V4 | create refresh tokens | Create the `refresh_tokens` table |
| V6 | support email or phone authentication | Make `email` nullable, add an identifier-presence check constraint, and add a case-insensitive unique index on `email` |

Note: **V5** was allocated to an early database foundation migration for the future `properties` table (schema and entity only — no repository queries, service, or API yet). Because that version number was already in use, the email-or-phone authentication migration was assigned **V6** instead of V5 to preserve Flyway's strict version ordering. Beyond that schema foundation, property-related feature migrations (images, amenities, favorites, inquiries) have not started yet.