# docs/features/02-user-role-foundation.md

# Feature: User and Role Foundation

**Status:** Completed

## Purpose

Establish the core identity model: the roles a user can have, and the user table that will support every future feature requiring authentication.

## Main Files/Components Involved

- `User` entity
- `Role` entity
- `UserRepository`
- `RoleRepository`
- `RoleName`, `AuthProvider`, `SellerStatus`, `AccountStatus` enums

## Database Changes

- V1: creates the `roles` and `users` tables
- V2: seeds the USER, SELLER, and ADMIN roles
- V3: adds a unique constraint on `phone_number`

## API Changes

None — no controllers or endpoints were introduced in this step.

## Business Rules

- Every user has exactly one role.
- Roles are limited to USER, SELLER, and ADMIN.
- `seller_status` is null until a user applies to become a seller.

## Security Considerations

- No authentication logic exists yet at this stage; this is purely the data foundation.

## Testing Checklist

- [x] `roles` and `users` tables created successfully
- [x] USER, SELLER, and ADMIN roles seeded
- [x] Unique phone number constraint verified in PostgreSQL

## Related Commit Message
