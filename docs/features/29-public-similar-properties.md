# Feature: Public Similar Properties API

- **Status:** Completed
- **Branch:** `feature/public-similar-properties`
- **Related Commit:** `feat: add similar properties api`

## Purpose
Provides a tailored list of comparable properties to display at the bottom of the Property Detail Page. This enhances user engagement by offering relevant alternatives based on the property type and location of the currently viewed listing.

## Endpoint & Security
- **Path:** `GET /api/v1/public/properties/{propertyId}/similar`
- **Authorization:** None (Publicly accessible).

## Visibility & Similarity Rules
1. **ACTIVE Only:** Inherits the core marketplace security rule; only properties with an `ACTIVE` status are eligible.
2. **Exclusion:** The reference property (`propertyId`) is explicitly excluded from the results to prevent recursive displays.
3. **Matching Criteria:** Finds properties sharing the exact same `propertyType` and `province`.
4. **Limits:** Hard-capped at 4 properties, ordered by the newest listings (`createdAt DESC`).

## Testing Checklist
- [ ] Fetch similar properties for an `ACTIVE` property -> 200 OK (max 4 results).
- [ ] Ensure the returned results all share the same `propertyType` and `province` as the reference property.
- [ ] Verify the reference property ID is *not* present in the returned list.
- [ ] Fetch similar properties for a `DRAFT` or `PENDING` property -> 404 Not Found.
- [ ] Verify endpoint functions properly without passing a JWT.