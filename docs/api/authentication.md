# docs/api/authentication.md

# Authentication API

Base path: `/api/v1/auth`

All responses use the standard `ApiResponse` envelope:

```json
{
  "success": true,
  "message": "Description of the result.",
  "data": {}
}
```

---

## POST /auth/register

**Purpose:** Create a new USER account using either an email address or a Cambodia phone number as the identifier.

**Authentication:** Not required (public endpoint).

**Request (email identifier):**

```json
{
  "full_name": "Sok Dara",
  "identifier": "sok.dara@example.com",
  "password": "Password@123",
  "password_confirmation": "Password@123"
}
```

**Request (phone identifier):**

```json
{
  "full_name": "Sok Dara",
  "identifier": "012345678",
  "password": "Password@123",
  "password_confirmation": "Password@123"
}
```

**Success response (201 Created):**

```json
{
  "success": true,
  "message": "Registration successful.",
  "data": {
    "user": {
      "id": 25,
      "full_name": "Sok Dara",
      "email": "sok.dara@example.com",
      "phone_number": null,
      "role": "USER",
      "seller_status": null,
      "auth_provider": "LOCAL"
    },
    "access_token": "JWT_ACCESS_TOKEN",
    "token_type": "Bearer",
    "expires_in": 900000
  }
}
```

**Possible errors:**

| Status | Message |
|---|---|
| 400 | "Password confirmation does not match." |
| 409 | "Email already exists." |
| 409 | "Phone number already exists." |

**Business rules:**

- Public registration always assigns role USER, auth_provider LOCAL, and account_status ACTIVE.
- Only one identifier field is accepted; the backend detects whether it is an email or a Cambodia phone number.
- Email is normalized to lowercase; phone numbers are normalized to E.164 format (e.g. `012345678` becomes `+85512345678`).
- Password is hashed with BCrypt before storage; `password_confirmation` is never stored.

---

## POST /auth/login

**Purpose:** Authenticate an existing user with either identifier type and issue an access token.

**Authentication:** Not required (public endpoint).

**Request (email identifier):**

```json
{
  "identifier": "sok.dara@example.com",
  "password": "Password@123",
  "remember_me": true
}
```

**Request (phone identifier):**

```json
{
  "identifier": "+85512345678",
  "password": "Password@123",
  "remember_me": false
}
```

**Success response (200 OK):**

```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "user": {
      "id": 25,
      "full_name": "Sok Dara",
      "email": "sok.dara@example.com",
      "phone_number": null,
      "role": "USER",
      "seller_status": null,
      "auth_provider": "LOCAL"
    },
    "access_token": "JWT_ACCESS_TOKEN",
    "token_type": "Bearer",
    "expires_in": 900000
  }
}
```

**Possible errors:**

| Status | Message |
|---|---|
| 401 | "Invalid credentials." |
| 403 | "This account is inactive." |

**Business rules:**

- The same identifier normalization rules from registration apply during login lookup.
- The response message is identical whether the identifier does not exist or the password is wrong, to avoid revealing account existence.
- A `refresh_token` HttpOnly cookie is set on successful login; it is never included in the JSON body.

---

## POST /auth/refresh

**Purpose:** Issue a new short-lived access token using the refresh token stored in the browser's cookie.

**Authentication:** Requires a valid `refresh_token` cookie (no Authorization header needed).

**Request:** No request body required.

**Success response (200 OK):**

```json
{
  "success": true,
  "message": "Token refreshed successfully.",
  "data": {
    "access_token": "NEW_JWT_ACCESS_TOKEN",
    "token_type": "Bearer",
    "expires_in": 900000
  }
}
```

**Possible errors:**

| Status | Message |
|---|---|
| 401 | Returned if the cookie is missing, invalid, expired, or revoked |

**Business rules:**

- The old refresh token is revoked and a new one issued (rotation) on every successful refresh.
- The `remember_me` duration chosen at login is preserved across refreshes.

---

## POST /auth/logout

**Purpose:** End the current session by revoking the refresh token.

**Authentication:** Requires a `refresh_token` cookie if one exists; the request still succeeds if it is missing.

**Request:** No request body required.

**Success response (200 OK):**

```json
{
  "success": true,
  "message": "Logout successful.",
  "data": null
}
```

**Business rules:**

- The refresh token is revoked in the database so it can never be used again, even if the raw cookie value were somehow retained.
- The `refresh_token` cookie is cleared in the response.