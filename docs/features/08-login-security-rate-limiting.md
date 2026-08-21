# docs/features/08-login-security-rate-limiting.md

# Feature: Login Security and IP Rate Limiting

**Status:** Completed (pending final test verification)

**Branch:** feature/login-security-rate-limiting

## Purpose

Protect the login endpoint against brute-force password guessing by tracking failed attempts per IP address and per identifier, and temporarily blocking either one after repeated failures — without ever storing plaintext IPs or identifiers, and without revealing to an attacker whether a given identifier exists.

## Login Security Flow

Login Request
↓

Is IP or identifier currently blocked?
── Yes ──> 429 Too Many Requests (Retry-After header)


↓ No
Verify credentials
↓


Correct? ── No ──> Record failed attempt (IP + identifier) ──> 401 Invalid credentials
↓ Yes


Reset failed attempts (IP + identifier)
↓


Account active? ── No ──> 403 This account is inactive
↓ Yes

Issue access token + refresh token ──> 200 Login successful


## Database Table: login_security_records

| Column | Purpose |
|---|---|
| `key_type` | Whether this row tracks an IP address or an identifier (email/phone) |
| `key_hash` | HMAC-SHA256 hash of the raw IP or identifier; the raw value is never stored |
| `failed_attempts` | Number of consecutive failures within the current window |
| `window_started_at` | When the current failure-counting window began |
| `last_failed_at` | Timestamp of the most recent failure |
| `blocked_until` | If set and in the future, this key is currently blocked |

## Security Rules

- 5 failed attempts within 15 minutes trigger a 24-hour block.
- Both the client IP and the normalized identifier are tracked independently.
- A block on either the IP or the identifier is enough to reject the login attempt.
- Blocking is checked before password verification, so no BCrypt comparison work is wasted on a blocked request.

## IP Block Policy

Prevents a single attacker machine from brute-forcing any account by rate-limiting based on where the requests are coming from.

## Identifier Block Policy

Prevents a distributed attack (many different IPs targeting one account) by also rate-limiting based on the account being targeted, regardless of which IP each attempt came from.

## HTTP 401 vs HTTP 429

| Status | Meaning |
|---|---|
| 401 Unauthorized | The credentials submitted in this specific request were wrong |
| 429 Too Many Requests | Too many wrong attempts have already occurred; this request is rejected without even checking the password |

## Client IP Resolver Policy

By default, the resolver uses the connection's actual remote address (`HttpServletRequest.getRemoteAddr()`), which is safe and cannot be spoofed by the client. The `X-Forwarded-For` header is only consulted when `app.security.trust-proxy-headers` is explicitly set to `true`. This flag must remain `false` until a production reverse proxy is configured to reliably set that header itself and strip any client-supplied value — otherwise an attacker could fake the header to bypass IP-based rate limiting entirely.

## Environment Variables

| Variable | Purpose |
|---|---|
| `RATE_LIMIT_HASH_SECRET` | Secret key used to HMAC-hash IP addresses and identifiers before storage |

## Testing Checklist

- [ ] A valid login succeeds normally
- [ ] An invalid login returns 401 with the generic "Invalid credentials." message
- [ ] 5 failed logins from the same IP within 15 minutes trigger a block
- [ ] A blocked login attempt returns 429 with the generic rate-limit message
- [ ] The 429 response includes a `Retry-After` header with remaining seconds
- [ ] A successful login resets both the IP and identifier failure counters

## Related Commit Message
