# docs/features/11-seller-request-approval-otp.md

# Feature: Seller Request Approval and Guest OTP Activation

**Status:** Completed (pending final test verification)

**Branch:** feature/seller-request-approval-otp

## Purpose

Give admins the ability to approve or reject seller requests, promoting existing USER accounts to SELLER instantly, or sending guests a one-time SMS code so they can create and activate their own SELLER account safely, without an admin ever handling their password.

## Existing USER Approval Flow

ADMIN approves PENDING request (user_id present)

↓

Linked User's role: USER → SELLER

↓

Seller request status: PENDING → APPROVED

↓

reviewed_at / reviewed_by recorded

↓

User can log in as SELLER immediately with existing credentials


## Guest Approval and OTP Activation Flow

ADMIN approves PENDING request (user_id = NULL)

↓

Check phone/email not already used by another User

↓

6-digit OTP generated, hashed, sent via SmsSender

↓

Seller request status: PENDING → APPROVED_PENDING_ACTIVATION

↓

Guest calls POST /seller-requests/{id}/activate with OTP + chosen password

↓

OTP verified (max 5 attempts, 10-minute expiry)

↓

New User created with SELLER role, phone/email from the request

↓

Seller request status: APPROVED_PENDING_ACTIVATION → APPROVED

↓

Guest logs in normally via POST /auth/login


## Seller Request State Transitions

| From | To | Trigger |
|---|---|---|
| PENDING | APPROVED | Existing USER approval |
| PENDING | APPROVED_PENDING_ACTIVATION | Guest approval |
| PENDING | REJECTED | Admin rejection |
| APPROVED_PENDING_ACTIVATION | APPROVED | Successful OTP activation |

Any other transition attempt returns `409 Conflict`.

## OTP Security Rules

- Generated using `SecureRandom`, always exactly 6 digits.
- Only a BCrypt hash of the OTP is stored — the raw code is never persisted.
- Expires 10 minutes after generation.
- Maximum 5 verification attempts before it becomes permanently unusable.
- Marked `consumed_at` once successfully used, and can never be reused afterward.
- Never appears in any API response, log (outside the development SMS sender), or exception message.

## OTP Resend Policy

- Admin-only, restricted to guest requests currently in `APPROVED_PENDING_ACTIVATION`.
- Maximum 3 total OTP sends (initial + resends) per seller request within a rolling 1-hour window.
- Exceeding the limit returns `429 Too Many Requests`.
- Each resend invalidates the previous active OTP before issuing a new one.

## SMS Development Sender Limitation

This project does **not** implement real SMS delivery. `DevelopmentSmsSender` only logs a structured line (`SELLER_ACTIVATION_OTP phone=<masked> code=<otp>`) and is active exclusively when both the `dev`/`test` Spring profile and `app.sms.provider=development` are set. In any other configuration — including the current production setup — no SMS sender bean exists, and any attempt to approve a guest seller request fails immediately with `503 Service Unavailable` and the message "SMS delivery is not configured." Integrating a real provider like Twilio or AWS SNS is planned for a future step and is explicitly out of scope here.

## API Endpoints

| Method | Endpoint | Access |
|---|---|---|
| POST | /api/v1/admin/seller-requests/{id}/approve | ADMIN only |
| POST | /api/v1/admin/seller-requests/{id}/reject | ADMIN only |
| POST | /api/v1/admin/seller-requests/{id}/resend-activation-otp | ADMIN only |
| POST | /api/v1/seller-requests/{id}/activate | Public |

## Authorization Rules

- All `/api/v1/admin/seller-requests/**` endpoints require the `ADMIN` role, enforced via `SecurityConfig`.
- `POST /seller-requests/{id}/activate` is public since the guest has no account yet — its security instead comes entirely from the OTP verification step.

## Database Table: seller_request_otp_codes

| Column | Purpose |
|---|---|
| `seller_request_id` | Which seller request this OTP belongs to |
| `code_hash` | BCrypt hash of the 6-digit code — never the raw code |
| `attempt_count` / `max_attempts` | Enforces the 5-attempt limit |
| `expires_at` | 10-minute validity window |
| `consumed_at` | Set once successfully used |
| `invalidated_at` | Set when superseded by a resend |

## Testing Checklist

- [ ] Existing USER seller request approval promotes the user to SELLER
- [ ] Guest seller request approval sends an OTP and sets status to APPROVED_PENDING_ACTIVATION
- [ ] Guest activation with a correct OTP creates a SELLER account and sets status to APPROVED
- [ ] Activation with an incorrect OTP returns 400 and increments the attempt count
- [ ] Activation with an expired OTP returns 400
- [ ] The 6th incorrect OTP attempt returns 429
- [ ] The 4th OTP resend within an hour returns 429
- [ ] Rejecting a PENDING request with a valid admin note works and sets REJECTED
- [ ] Non-admin users receive 403 on all admin seller-request endpoints

## Related Commit Message
