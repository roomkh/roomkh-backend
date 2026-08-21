# docs/features/05-jwt-authentication.md

# Feature: JWT Authentication

**Status:** Completed

## Purpose

Allow registered users to log in and access protected endpoints using a stateless, signed token.

## Main Files/Components Involved

- `AuthController` (login endpoint)
- `JwtService`
- `JwtAuthenticationFilter`
- `SecurityConfig`
- `CustomUserDetails` / `CustomUserDetailsService`
- `JwtAuthenticationEntryPoint`
- `CustomAccessDeniedHandler`

## Database Changes

None.

## API Changes

- `POST /auth/login` added

## Business Rules

- Login requires a matching, active LOCAL account.
- Passwords are verified using BCrypt comparison.

## Security Considerations

- Sessions are fully stateless; no server-side session storage is used.
- CSRF protection is disabled, appropriate for a token-based REST API rather than a cookie/session-based web app.
- Form login and HTTP Basic authentication are disabled since JWT is the only supported authentication mechanism.
- Unauthenticated requests receive 401; authenticated-but-unauthorized requests receive 403, each returned in the standard `ApiResponse` format.

## Testing Checklist

- [x] Login succeeds with correct credentials
- [x] Login fails with incorrect credentials
- [x] Protected endpoints reject requests without a token (401)
- [x] Protected endpoints accept requests with a valid token

## Related Commit Message
