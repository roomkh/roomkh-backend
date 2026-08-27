# Feature: Public Property List API

- **Status:** Completed
- **Branch:** `feature/public-property-list`
- **Related Commit:** `feat: add public property list and search api`

## Purpose
Provides the primary search and listing endpoint for the frontend marketplace. It allows unauthenticated users to browse and filter available properties.

## Endpoint & Security
- **Path:** `GET /api/v1/public/properties`
- **Authorization:** None (Publicly accessible).

## Visibility Rule (Critical Security Measure)
This endpoint implements a hardcoded database predicate (`status = ACTIVE`) using JPA Specifications. Properties in `DRAFT`, `PENDING`, `REJECTED`, or `SOLD_RENTED` statuses are completely isolated from this API, ensuring users only see approved and available listings.

## Supported Filters & Sorting
- **Filters:** `purpose` (RENT/SALE), `property_type`, `min_price`, `max_price`, `province`.
- **Sorting (`sort_by`):**
    - `newest` (Default): Orders by creation/listing date descending.
    - `price_asc`: Orders by price ascending.
    - `price_desc`: Orders by price descending.
- **Pagination:** Uses `page` and `size` parameters.

## Testing Checklist
- [ ] Fetch without filters -> 200 OK (Returns ONLY `ACTIVE` properties).
- [ ] Verify `DRAFT` or `PENDING` properties do not appear in the results.
- [ ] Apply `min_price` and `max_price` filters -> Results fall within the range.
- [ ] Apply `purpose` and `property_type` filters -> Results match exactly.
- [ ] Sorting by `price_asc` and `price_desc` applies correct ordering.
- [ ] Ensure the endpoint works without passing an Authorization header (JWT).