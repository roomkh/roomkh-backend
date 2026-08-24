# docs/features/13-seller-property-draft-creation.md

# Feature: Seller Property Draft Creation

**Status:** Completed (pending final test verification)

**Branch:** feature/seller-property-draft-creation

## Purpose

Allow an authenticated SELLER to create a new property listing that starts safely in DRAFT status, with strict server-side control over ownership, status, and moderation fields.

## Endpoint and Authentication

`POST /api/v1/seller/properties` — requires a valid JWT with the SELLER role. USER and ADMIN tokens receive 403 Forbidden; missing/invalid tokens receive 401 Unauthorized.

## DRAFT-Only Creation Rule

Every property created through this endpoint is forced to `DRAFT` status server-side. The optional `status` field in the request is validated to only ever accept the literal value `DRAFT` (or be omitted) — any other value fails validation before it ever reaches the service layer, and even a valid `"DRAFT"` value is never actually read by the service, which hardcodes `PropertyStatus.DRAFT` regardless.

## Ownership Security Rule

The property owner (`seller`) is derived exclusively from the authenticated JWT's user ID — never from any client-supplied field. `seller_id`, `owner_id`, `user_id`, `reviewed_by`, `view_count`, `inquiry_count`, `is_featured`, `listed_at`, `reviewed_at`, and `rejection_reason` are all completely absent from the request DTO, so there is no client-controlled path to set them.

## Request Fields and Validation Rules

| Field | Required | Rule |
|---|---|---|
| title | Yes | Not blank, max 255 characters |
| property_type | Yes | ROOM, HOME, APARTMENT, VILLA, or LAND |
| purpose | Yes | RENT or SALE |
| price | Yes | Greater than 0, max 2 decimal places |
| currency | No | Defaults to USD, normalized to uppercase, must be 3 letters |
| price_unit | Yes | MONTH or SELL, must match purpose (see below) |
| province / district / commune | Yes | Not blank, max 150 characters each |
| address | No | Max 500 characters |
| latitude / longitude | No | Both present or both null; valid geographic ranges |
| description | Yes | 20–10000 characters |
| bedrooms / bathrooms / floor / age_years | No | Null or ≥ 0 |
| size_sqm | No | Null or > 0 |
| furnished | No | Defaults to false |
| amenity_codes | No | Must exist in the amenities table; no blanks or duplicates |
| status | No | Must be omitted or exactly "DRAFT" |

## Property Type, Purpose, and Price Unit Rules

`RENT` properties must use `price_unit: MONTH`; `SALE` properties must use `price_unit: SELL`. Any mismatch returns a `400` validation error pointing at the `priceUnit` field.

## Location and Map Coordinate Rules

Province, district, and commune are required free-text fields from frontend dropdowns. Address, latitude, and longitude are optional — but latitude and longitude must be supplied together or not at all, enforced by a custom class-level validator, matching the same rule already enforced at the database level from Step 8.1.

## Amenity Validation Rule

Submitted amenity codes are trimmed and uppercased, checked for blanks and duplicates, then verified to exist in the `amenities` table. Any unknown code returns a clear `400` error listing exactly which code(s) were not recognized.

## Slug Generation Rule

Slugs are generated entirely server-side from the property title, lowercased, stripped of non-Latin characters, hyphenated, and appended with a short random unique suffix (e.g., `studio-room-in-bkk1-a7f3c9d2`), with uniqueness confirmed against the database before saving. If a title produces no usable Latin characters, a safe fallback like `property-a7f3c9d2` is used instead.

## Seeded Default Amenities

| Code | Name |
|---|---|
| AIR_CONDITIONING | Air Conditioning |
| WIFI | WiFi |
| BALCONY | Balcony |
| PARKING | Parking |
| SECURITY_24H | 24/7 Security |

## Response Format

Returns `201 Created` with the created property's public-safe fields (id, title, slug, type, purpose, price, location, amenities, timestamps) — internal fields like `seller` details, `reviewed_by`, `is_featured`, `view_count`, `inquiry_count`, and `rejection_reason` are never included in the response.

## Explicit Out-of-Scope Features

Not built in this step: property update, delete, submit-for-review, status changes, admin property review, public property list/detail/search, property images, Cloudinary, locations API, map provider integration, favorites, inquiries, and seller dashboard.

## Testing Checklist

- [ ] SELLER can create a property draft successfully
- [ ] USER is rejected with 403
- [ ] ADMIN is rejected with 403
- [ ] Request without a JWT is rejected with 401
- [ ] RENT + SELL combination returns a validation error
- [ ] Latitude without longitude returns a validation error
- [ ] Unknown amenity code returns a clear 400 error
- [ ] Duplicate amenity codes in the same request are rejected
- [ ] Sending status other than DRAFT is rejected
- [ ] Omitting all optional fields still succeeds with correct defaults

## Related Commit Message
