# Feature: Seller Submit Property for Review API

- **Status:** Completed
- **Branch:** `feature/seller-submit-property`
- **Related Commit:** `feat: add submit property for admin review api`

## Purpose
Enables an authenticated seller to submit a `DRAFT` or `REJECTED` property for Admin review (the "Save & Publish" UI action). This action updates the property status to `PENDING` and records the submission time.

## Endpoint & Authorization
- **Path:** `POST /api/v1/seller/properties/{propertyId}/submit`
- **Authorization:** `Bearer {JWT}`
- **Role Requirement:** `SELLER`

## Key Business Rules
1. **Ownership Validation:** Property must exist and belong to the authenticated seller context.
2. **State Machine Rule:** Status transitions permitted only from `DRAFT` or `REJECTED` to `PENDING`.
3. **Image Rules:**
    - Property must have at least one uploaded image.
    - Property must have exactly one active cover image (`is_cover = true`).
4. **Concurrency Control:** Utilizes `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findByIdAndSellerIdForUpdate`) to prevent concurrent updates or submissions while executing state changes.

## Database Changes
- **Migration:** `V13__add_submitted_at_to_properties_table.sql` added `submitted_at` column to the `properties` table.

## Testing Checklist
- [ ] SELLER submits valid DRAFT property with cover image -> 200 OK
- [ ] SELLER submits valid REJECTED property -> 200 OK
- [ ] Submit property with 0 images -> 400 Bad Request
- [ ] Submit property with images but no cover -> 400 Bad Request
- [ ] Submit property already PENDING/ACTIVE/SOLD_RENTED -> 400 Bad Request
- [ ] Submit another seller's property -> 404 Not Found
- [ ] USER/ADMIN role access -> 403 Forbidden