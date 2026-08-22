# docs/features/09-seller-request-foundation.md

# Feature: Seller Request Foundation

**Status:** Completed (pending final test verification)

**Branch:** feature/seller-request-foundation

## Purpose

Allow both existing USER accounts and anonymous visitors to submit a request to become a seller, without yet granting the SELLER role or activating any account. This step only captures the request data and gives admins a queryable list to review manually later.

## Guest Seller Request Flow

Visitor (no account)


↓ 

POST /seller-requests (no JWT)
seller_requests row created
user_id = NULL
status = PENDING


## Existing USER Seller Request Flow

Logged-in USER

↓ 

POST /seller-requests (with JWT)
Backend reads user ID from JWT (never from request body)

↓


seller_requests row created
user_id = authenticated user's ID
status = PENDING
User role remains USER (no promotion yet)


## Database Table: seller_requests

| Column Group | Purpose |
|---|---|
| Applicant info | `full_name`, `email`, `phone_number`, `position`, `business_name`, `reason` |
| Ownership | `user_id` (null for guests) |
| Status tracking | `status`, `admin_note` |
| Future admin workflow | `contacted_at`, `contacted_by`, `reviewed_at`, `reviewed_by` (populated in a later step) |

## Seller Request Statuses

| Status | Meaning |
|---|---|
| PENDING | Newly submitted, awaiting admin action (the only status this step produces) |
| APPROVED_PENDING_ACTIVATION | Reserved for a future step involving SMS OTP account activation |
| APPROVED | Reserved for a future step where the seller role is actually granted |
| REJECTED | Reserved for a future step |

## Duplicate Pending Request Policy

| Requester Type | Duplicate Check | Response |
|---|---|---|
| Logged-in USER | One PENDING request per `user_id` | 409 "You already have a pending seller request." |
| Guest | One PENDING request per normalized `phone_number` | 409 "A pending seller request already exists for this phone number." |

## API Endpoints

| Method | Endpoint | Access |
|---|---|---|
| POST | /api/v1/seller-requests | Public (JWT optional) |
| GET | /api/v1/admin/seller-requests | ADMIN only |
| GET | /api/v1/admin/seller-requests/{id} | ADMIN only |

## Security Rules

- The request body can never set `user_id` — it is derived exclusively from the JWT if present.
- SELLER and ADMIN accounts are rejected from submitting a new seller request (HTTP 400).
- Admin endpoints require the ADMIN role via Spring Security URL matching.

## Testing Checklist

- [ ] Guest can submit a seller request without a JWT
- [ ] Logged-in USER can submit a seller request; `user_id` is attached automatically
- [ ] SELLER or ADMIN attempting to submit a seller request receives 400
- [ ] A second PENDING request from the same logged-in user returns 409
- [ ] A second PENDING request from the same guest phone number returns 409
- [ ] Admin list endpoint returns paginated results with correct `meta`
- [ ] Admin detail endpoint returns the full seller request object
- [ ] Non-admin users receive 403 when calling admin endpoints

## Related Commit Message
