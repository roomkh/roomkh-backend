# docs/security/jwt-authentication.md

# JWT Authentication

## What JWT Authentication Means in RoomKH

Every authenticated request to the RoomKH backend must include a signed JSON Web Token (JWT) proving who the caller is. The server never stores session state in memory — each request is verified independently using only the token, which is what makes the API stateless and horizontally scalable.

## Access Token Flow

1. The user registers or logs in successfully.
2. The backend generates a JWT access token signed with a server-side secret.
3. The token is returned in the JSON response body (never in a cookie).
4. The frontend includes the token on every subsequent request using the header:

5. A filter on the backend verifies the token's signature and expiry on every request before allowing access to protected endpoints.

## JWT Subject

The JWT's subject claim contains the user's numeric ID (not their email or phone number). This keeps the token's identity claim stable even if a user's email or phone number changes later.

## Public vs Protected Endpoints

| Endpoint | Access |
|---|---|
| POST /auth/register | Public |
| POST /auth/login | Public |
| POST /auth/refresh | Public (relies on cookie, not JWT) |
| POST /auth/logout | Public (relies on cookie, not JWT) |
| All other endpoints | Require a valid JWT |

## 401 vs 403

| Status | Meaning |
|---|---|
| 401 Unauthorized | The request has no valid identity — the token is missing, malformed, or expired |
| 403 Forbidden | The request has a valid identity, but that identity is not allowed to perform the action |

## Password Hashing

User passwords are hashed with BCrypt before being stored. BCrypt is a one-way, salted hashing algorithm, meaning the original password can never be recovered from the stored hash, and even two users with an identical password will have different hash values in the database.
## Login Rate Limiting

The login endpoint now enforces IP-based and identifier-based rate limiting: 5 failed attempts within 15 minutes trigger a 24-hour block, returning HTTP 429 with a `Retry-After` header. See [Login Security and Rate Limiting](../features/08-login-security-rate-limiting.md) for full details.