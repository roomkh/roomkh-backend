# Feature: Admin Property List API

- **Status:** Completed
- **Branch:** `feature/admin-property-list`
- **Related Commit:** `feat: add admin property list api`

## Purpose
Provides a dedicated endpoint for platform administrators to fetch a paginated list of properties across the entire platform. This is primarily used to populate an admin review dashboard by filtering properties with a `PENDING` status.

## Endpoint & Authorization
- **Path:** `GET /api/v1/admin/properties`
- **Authorization:** `Bearer {JWT}`
- **Role Requirement:** `ADMIN`

## Key Business Rules
1. **Security Isolation:** Enforces strict role checks (`@PreAuthorize("hasRole('ADMIN')")`). `SELLER` and `USER` roles are denied access (HTTP 403).
2. **Filtering & Sorting:**
    - Supports filtering by `PropertyStatus` via the `status` query parameter.
    - Provides specialized sorting profiles (e.g., `sort_by=recently_submitted` to push the latest review requests to the top of the queue).
3. **Optimized Queries:**
    - Runs in a `@Transactional(readOnly = true)` context.
    - Mitigates N+1 query problems by fetching `PropertyImage` entities in bulk for the current page using `findByProperty_IdInAndCoverTrue`.
4. **Tailored Payload:** The response is flattened and tailored for admin workflows, including direct nested references to the `owner` details (ID, name, email, phone) to facilitate easy contact.

## Testing Checklist
- [ ] ADMIN requests all properties -> 200 OK
- [ ] ADMIN filters by `status=PENDING` -> 200 OK with only PENDING records
- [ ] ADMIN uses `sort_by=recently_submitted` -> 200 OK with correct ordering
- [ ] Response includes the correct `owner_*` fields and `cover_image_url`
- [ ] SELLER role access -> 403 Forbidden
- [ ] USER role access -> 403 Forbidden
- [ ] Request without JWT -> 401 Unauthorized