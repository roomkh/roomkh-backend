# docs/features/10-seller-request-rate-limiting.md

# Feature: Seller Request Rate Limiting

**Status:** Completed (pending final test verification)

**Branch:** feature/seller-request-rate-limiting

## Purpose

Protect `POST /seller-requests` — a publicly accessible endpoint that accepts anonymous submissions — from spam bots, scripted floods, and abusive repeat submissions from a single account.

## Seller Request Abuse Prevention Flow

POST /seller-requests

↓

Hard IP flood check (100 req / 1 second)

↓ pass

Soft IP limit check (10 req / 1 minute)

↓ pass

Authenticated USER daily check (3 req / 24 hours, only if logged in)

↓ pass

Existing duplicate PENDING request check (unchanged from Step 7.3)

↓ pass

Seller request created


Any failed check immediately returns 429 and stops the request before it reaches the duplicate-check or database-write stage.

## Soft IP Limit Policy

- Maximum 10 attempts per IP within a rolling 1-minute window.
- Exceeding it returns 429 but does **not** trigger a 24-hour block — the counter simply resets once the minute window passes.

## Hard IP Flood Block Policy

- 100 attempts per IP within 1 second indicates automated/bot traffic, not a human.
- Reaching this threshold blocks the IP for 24 hours, returning 429 on every request during that period regardless of the soft limit's own window.

## Authenticated USER Daily Limit Policy

- A logged-in USER can submit at most 3 seller requests within a rolling 24-hour window, regardless of which IP address they use.
- This exists independently of the existing "one PENDING request at a time" rule from Step 7.3 — both checks apply together.

## HTTP 429 and Retry-After

Every rejection returns the same generic body:

```json
{
  "success": false,
  "message": "Too many seller request attempts. Please try again later.",
  "data": null
}
```

The `Retry-After` header tells the client exactly how many seconds to wait — either until the current 1-minute/24-hour window resets, or until a 24-hour IP block expires, without ever exposing the actual attempt count or block timestamp in the response body.

## Database Table: seller_request_rate_limit_records

| Column | Purpose |
|---|---|
| `key_type` | Whether this row tracks an IP address or a USER ID |
| `key_hash` | HMAC-SHA256 hash of the raw IP or user ID — never stored in plain text |
| `window_type` | Which policy this row belongs to: SECOND (flood), MINUTE (soft), or DAY (per-user) |
| `request_count` | Attempts counted in the current window |
| `window_started_at` | When the current counting window began |
| `blocked_until` | Only set for the hard flood policy; if in the future, this key is blocked |

## Privacy Rule for Hashed Records

Neither the raw client IP address nor the raw user ID is ever written to the database — only their HMAC-SHA256 hash, using the same `RATE_LIMIT_HASH_SECRET` already established for login security. This means even direct database access cannot reveal which specific IP or user triggered a given rate-limit record.

## Concurrency Safety

Each rate-limit check uses a PostgreSQL row-level lock (`SELECT ... FOR UPDATE` via JPA's `@Lock(PESSIMISTIC_WRITE)`) inside a single database transaction. This guarantees that if two requests from the same IP arrive at the exact same instant, one waits for the other to finish updating the counter before reading it — preventing both requests from seeing a stale count and both being incorrectly allowed through. Counters and blocks are stored in PostgreSQL, not in application memory, so they survive an application restart.

## Testing Checklist

- [ ] A normal guest seller request succeeds
- [ ] The 11th request from the same IP within a minute returns 429 without a 24-hour block
- [ ] 100 rapid requests within 1 second trigger a 24-hour IP block
- [ ] A logged-in USER's 4th seller request within 24 hours returns 429
- [ ] A blocked IP receives 429 on every subsequent attempt until the block expires
- [ ] Every 429 response includes a correct `Retry-After` header

## Related Commit Message
