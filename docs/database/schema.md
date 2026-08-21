# docs/database/schema.md

# Database Schema

This document covers only the tables that currently exist in the database. Tables for future features (such as properties, favorites, or inquiries) are documented separately once their APIs are implemented — the `properties` table has a database migration and entity in place as early foundation work, but has no repository logic beyond a basic lookup, no service layer, and no API yet, so it is intentionally excluded from this document until that feature is completed.

## Tables

### roles

Stores the fixed set of roles a user can have in the system.

| Purpose |
|---|
| Defines the three system roles: USER, SELLER, ADMIN |

### users

Stores every registered account, whether identified by email, phone number, or both.

| Purpose |
|---|
| Central identity table for authentication, authorization, and account status |

### refresh_tokens

Stores hashed refresh tokens used to issue new access tokens without requiring re-login.

| Purpose |
|---|
| Enables long-lived sessions (remember me) and token revocation on logout |

## Relationships


- A `User` belongs to exactly one `Role`.
- A `RefreshToken` belongs to exactly one `User`.

## Important User Fields

| Field | Purpose |
|---|---|
| `full_name` | The user's display name |
| `email` | Email identifier; nullable to support phone-only accounts |
| `phone_number` | Cambodia phone identifier in E.164 format; nullable to support email-only accounts |
| `password` | BCrypt-hashed password, never stored in plain text |
| `role_id` | Foreign key linking the user to their assigned role |
| `auth_provider` | Distinguishes local password accounts from future OAuth accounts (currently LOCAL only is active) |
| `seller_status` | Tracks seller approval state; null until a user applies to become a seller |
| `account_status` | Whether the account is ACTIVE or INACTIVE |

`email` and `phone_number` together implement the email-or-phone authentication policy: a user registers and logs in with exactly one identifier, and a database-level constraint guarantees at least one of the two fields is always present.