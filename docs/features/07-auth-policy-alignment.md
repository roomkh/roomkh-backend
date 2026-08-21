# docs/features/07-auth-policy-alignment.md

# Feature: Email or Phone Authentication Policy Alignment

**Status:** Completed

## Purpose

Allow users to register and log in using either an email address or a Cambodia phone number as a single, unified identifier, while tightening what public registration is allowed to control.

## Main Files/Components Involved

- `RegisterRequest`, `LoginRequest`
- `UserResponse`, `UserMapper`
- `AuthServiceImpl` (identifier detection and normalization)
- `JwtService` (subject changed to user ID)
- `CustomUserDetails`, `CustomUserDetailsService`
- `JwtAuthenticationFilter`

## Database Changes

- V6: makes `email` nullable, adds a check constraint requiring at least one identifier (email or phone number), and adds a case-insensitive unique index on `email`

## API Changes

- `POST /auth/register` and `POST /auth/login` now accept a single `identifier` field instead of separate email/phone fields

## Business Rules

- Public registration always assigns role USER, auth_provider LOCAL, and account_status ACTIVE; these fields can never be supplied by the request body.
- An identifier containing `@` is treated as an email; otherwise it is treated as a Cambodia phone number.
- Email is normalized to lowercase; Cambodia phone numbers are normalized to E.164 format (for example, `012345678` becomes `+85512345678`).
- Both email and phone number must remain unique across users.
- Existing users who already had both an email and a phone number continue to work unchanged.
- ADMIN accounts do not have public forgot-password functionality; forgot password itself has not been implemented yet for any role.

## Security Considerations

- The JWT subject now stores the user's numeric ID instead of their email, keeping the token's identity claim independent of an identifier that could later change.
- Login failures always return the same generic "Invalid credentials." message regardless of whether the identifier was malformed, not found, or the password was wrong, preventing account enumeration.

## Testing Checklist

- [x] Registration succeeds with an email identifier
- [x] Registration succeeds with a Cambodia phone identifier
- [x] Duplicate email is rejected
- [x] Duplicate phone number is rejected (including differently formatted input for the same number)
- [x] Login succeeds with either identifier type
- [x] Invalid identifier or wrong password returns a generic "Invalid credentials." message
- [x] Existing access tokens and the refresh token flow continue to work after the JWT subject change

## Related Commit Message
