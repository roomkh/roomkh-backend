# docs/features/14-seller-property-list.md

# Feature: Seller Property List API

**Status:** Completed (pending final test verification)

**Branch:** feature/seller-property-list

## Purpose

Power the Seller Home page: paginated property listing, per-status tab counts, and sorting, scoped strictly to the authenticated seller's own properties.

## Endpoint and Authorization

`GET /api/v1/seller/properties` — requires a valid JWT with the SELLER role. USER and ADMIN tokens receive 403; missing/invalid tokens receive 401.

## Ownership Security Rule

The seller ID is derived exclusively from the JWT's authenticated user, never from a query parameter. Every list and count query is filtered by `seller_id` at the database level — a seller can never retrieve another seller's properties, regardless of which status filter is requested.

## Pagination Rules

`page` is 1-based (default 1, minimum 1) and internally converted to Spring Data's 0-based index. `size` defaults to 10 (minimum 1, maximum 50). An out-of-range page returns an empty `data` array with valid `meta`, never a 404.

## Status Filter Rules

Omitting `status` returns all of the seller's properties. Providing `status` filters to that status only. Invalid values return 400. `REJECTED` is supported as a direct filter even though it has no dedicated Seller Home tab yet.

## Status Count Rules

`meta.status_counts` always reflects **all** of the seller's properties (`all`, `active`, `pending`, `draft`, `sold_rented`) regardless of which `status` filter was applied to the main list — each count comes from a separate, indexed database `COUNT` query, never from loading and filtering records in Java.

## Sort Options

| API Value | Behavior |
|---|---|
| recently_updated (default) | `updated_at` descending |
| newest | `created_at` descending |
| price_asc | `price` ascending, then `updated_at` descending |
| price_desc | `price` descending, then `updated_at` descending |

## Property Card Response Fields

Each item includes title, slug, type, purpose, price/currency/price_unit, status, location (province/district/commune), bedrooms/bathrooms/size_sqm, view/inquiry counts, and timestamps. Seller PII, JWT/token data, review metadata, `is_featured`, and amenities are never included in this list response.

## Cover Image Null Behavior

`cover_image_url` is always `null` in this step since property image upload doesn't exist yet. The frontend is expected to fall back to its own placeholder image whenever this field is `null` — no fake or placeholder URLs are generated server-side.

## Empty Result Behavior

An out-of-range page or a status with zero matching properties returns `200 OK` with `data: []` and `meta.total: 0`, `meta.total_pages: 0` — never a 404.

## Explicit Out-of-Scope Features

Not built in this step: property update, delete, submit-for-review, status changes, admin property review, public property list/detail/search, property images, Cloudinary, locations API, map integration, favorites, inquiries, boost listing, and full seller dashboard statistics.

## Testing Checklist

- [ ] SELLER retrieves only their own properties
- [ ] Each status tab filter (ACTIVE, PENDING, DRAFT, SOLD_RENTED, REJECTED) returns correct results
- [ ] status_counts remains constant across different status filters for the same seller
- [ ] Empty result page returns 200 with empty data array
- [ ] All four sort_by values order results correctly
- [ ] Invalid status, sort_by, page, or size returns 400
- [ ] USER and ADMIN receive 403
- [ ] Request without JWT receives 401
- [ ] Seller A cannot see Seller B's properties

## Related Commit Message
