# docs/features/04-user-registration.md

# Feature: User Registration

**Status:** Completed

## Purpose

Allow new users to create an account with a securely hashed password.

## Main Files/Components Involved

- `AuthController` (register endpoint)
- `AuthService` / `AuthServiceImpl`
- `UserMapper`
- `PasswordConfig` (BCrypt bean)

## Database Changes

None beyond the foundation established in the User and Role Foundation feature.

## API Changes

- `POST /auth/register` added

## Business Rules

- New registrations are always assigned the USER role.
- Email and phone number must each be unique.
- Password and password confirmation must match; the confirmation value is never stored.
- The password is hashed with BCrypt before being saved.

## Security Considerations

- Passwords are never returned in any API response.
- BCrypt hashing ensures stored passwords cannot be reversed even if the database is compromised.

## Testing Checklist

- [x] Registration succeeds with valid data
- [x] Duplicate email is rejected
- [x] Duplicate phone number is rejected
- [x] Mismatched password confirmation is rejected
- [x] Response never contains the password field

## Related Commit Message
