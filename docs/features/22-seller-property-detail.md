# Feature: Seller Property Detail API

- **Status:** Completed
- **Branch:** `feature/seller-property-detail`
- **Related Commit:** `feat: add seller property detail api`

## Purpose
Enables an authenticated seller to fetch the complete dataset for their own property. This includes all basic information, physical attributes, location, metadata, related amenity codes, and image configurations. The primary frontend use case is populating the "Edit Property" form.

## Endpoint & Authorization
- **Path:** `GET /api/v1/seller/properties/{propertyId}`
- **Authorization:** `Bearer {JWT}`
- **Role Requirement:** `SELLER`

## Key Business Rules
1. **Ownership Validation:** The property must exist and belong to the authenticated seller (`owner_id = seller_id`). A standard `404 Not Found` is returned otherwise.
2. **Read-Only Transaction:** The service uses `@Transactional(readOnly = true)` to optimize database fetch operations.
3. **Nested Relations:**
    - `amenity_codes` maps the associated `Amenity` entities to a simple string array for easy frontend checkbox binding.
    - `images` maps the associated `PropertyImage` entities, strictly sorted by `sort_order` ascending, ensuring the frontend gallery renders correctly.

## Testing Checklist
- [ ] SELLER requests their own property -> 200 OK
- [ ] Response includes all expected top-level fields (price, location, etc.)
- [ ] Response includes `amenity_codes` array populated correctly
- [ ] Response includes `images` array sorted by `sort_order` ascending
- [ ] SELLER requests another seller's property -> 404 Not Found
- [ ] USER role access -> 403 Forbidden
- [ ] ADMIN role access -> 403 Forbidden
- [ ] Request without JWT -> 401 Unauthorized