# docs/features/06-refresh-token-remember-me.md

# Feature: Refresh Token, Remember Me, and Logout

**Status:** Completed

## Purpose

Let users stay logged in beyond the lifespan of a short-lived access token, without storing long-lived tokens in a way that is vulnerable to script-based theft.

## Main Files/Components Involved

- `RefreshToken` entity
- `RefreshTokenRepository`
- `RefreshTokenService` / `RefreshTokenServiceImpl`
- `RefreshTokenCookieUtil`
- `AuthController` (refresh and logout endpoints)

## Database Changes

- V4: creates the `refresh_tokens` table

## API Changes

- `POST /auth/refresh` added
- `POST /auth/logout` added
- `POST /auth/login` updated to accept `remember_me`

## Business Rules

- The raw refresh token is only ever sent to the client inside an HttpOnly cookie.
- Only a hashed version of the refresh token is stored in the database.
- Refresh tokens are rotated (old one revoked, new one issued) on every successful refresh.
- Logging out revokes the associated refresh token immediately.

## Security Considerations

- HttpOnly cookies prevent the refresh token from being read by JavaScript, mitigating XSS-based token theft.
- Hashing refresh tokens before storage means a database breach alone cannot be used to impersonate users.
- Token rotation limits how long a stolen refresh token would remain useful.

## Testing Checklist

- [x] Login with `remember_me: true` issues a long-lived cookie
- [x] Login with `remember_me: false` issues a session cookie
- [x] Refresh issues a new access token and rotates the refresh token
- [x] Logout revokes the refresh token
- [x] Refresh after logout returns 401

## Related Commit Message
