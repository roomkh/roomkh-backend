# docs/security/refresh-token.md

# Refresh Token Strategy

## Access Token vs Refresh Token

| | Access Token | Refresh Token |
|---|---|---|
| Format | JWT | Opaque random string |
| Lifespan | Short (minutes) | Long (up to 30 days, or session-length) |
| Storage | JSON response body, kept in frontend memory | HttpOnly cookie only |
| Purpose | Authorizes individual API requests | Obtains a new access token without re-login |

## Refresh Token Flow

1. On successful login or registration, the backend generates a random refresh token and a matching access token.
2. The raw refresh token is sent to the browser only inside an HttpOnly cookie — it is never included in the JSON response.
3. When the access token expires, the frontend calls `POST /auth/refresh`. The browser automatically attaches the refresh token cookie.
4. The backend validates the refresh token, issues a new access token and a new refresh token, and replaces the cookie (rotation).

## Why HttpOnly Cookies

An HttpOnly cookie cannot be read by JavaScript running in the browser, which protects the refresh token from theft through cross-site scripting (XSS) attacks. This is why refresh tokens are never returned in a JSON response body — putting a refresh token in JSON would let any script on the page read and exfiltrate it.

## Hashing in the Database

The raw refresh token value is never stored in the database. Only a one-way hash of the token is stored. If the database were ever compromised, the stored hashes alone could not be used to authenticate as any user.

## Remember Me Behavior

- If `remember_me` is `true` at login, the refresh token cookie persists for an extended period (up to 30 days), and the user stays logged in across browser restarts.
- If `remember_me` is `false`, the refresh token is issued with a shorter lifespan and the cookie is a session cookie, meaning it disappears when the browser is closed.

## Refresh Token Rotation

Every time `/auth/refresh` is called, the old refresh token is immediately revoked and a brand-new one is issued. This limits the damage window if a refresh token were ever stolen — it can only be used once before becoming invalid.

## Logout Revocation

Calling `/auth/logout` revokes the associated refresh token in the database immediately, so even if the cookie were somehow retained by a browser or intercepted beforehand, it can no longer be used to obtain new access tokens.