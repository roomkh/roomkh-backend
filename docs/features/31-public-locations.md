# Feature: Public Locations API

- **Status:** Completed
- **Branch:** `feature/public-locations`
- **Related Commit:** `feat: add public locations api`

## Purpose
Provides a lightweight endpoint to retrieve all distinct provinces that currently have active property listings. This allows the frontend to dynamically populate the location search dropdown, ensuring users only see and select provinces that actually have available properties (preventing zero-result searches).

## Logic & Visibility Rules
- **ACTIVE Only:** The database query strictly filters for properties where `status = 'ACTIVE'`.
- **Distinct Values:** Uses SQL `DISTINCT` to return unique province names.
- **Data Integrity:** Filters out `NULL` values at the database level and strips out empty/blank strings at the service level.

## Endpoint & Security
- **Path:** `GET /api/v1/public/locations`
- **Authorization:** None (Publicly accessible).

## Testing Checklist
- [ ] Send GET request -> 200 OK.
- [ ] Verify the response body is a JSON array of strings (e.g., `["Phnom Penh", "Siem Reap"]`).
- [ ] Ensure provinces with only `DRAFT` or `PENDING` properties do not appear in the list.
- [ ] Verify empty strings or nulls are not returned.
- [ ] Ensure the endpoint functions properly without a JWT.