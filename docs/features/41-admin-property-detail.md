# Feature: Admin Property Detail API

- **Status:** Completed
- **Branch:** `feature/admin-property-detail`
- **Related Commit:** `feat: add admin view property detail endpoint`

## Purpose
Provides a comprehensive view of a single property listing. This is specifically tailored for administrators to review all associated data (owner details, all images, amenities, and full descriptions) before executing moderation actions like approval or rejection.

## Logic & Mapping
- **Entity Resolution:** Looks up the `Property` by `id`, throwing a standard `ResourceNotFoundException` if no match is found.
- **Nested Collections:**
    - Iterates through `PropertyImage` to extract a flat list of `image_urls` and isolates the single `cover_image_url`.
    - Iterates through the `amenities` Set to generate a flat list of string names.
- **Data Formatting:** Combines `district` and `province` for the location string and applies the same `#LST-` and `OWN-` ID prefixes used in the admin list view for consistency.

## Endpoint & Security
- **Path:** `GET /api/v1/admin/properties/{id}`
- **Authorization:** Requires valid JWT and `ADMIN` role.

## Testing Checklist
- [ ] Make a GET request with a valid property ID and verify all nested objects (images, amenities, owner data) are present.
- [ ] Make a GET request with a non-existent ID to verify the 404 Not Found exception is thrown correctly.