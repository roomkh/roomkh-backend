# Feature: Admin Properties List API

- **Status:** Completed
- **Branch:** `feature/admin-properties-list`
- **Related Commit:** `feat: add admin paginated properties list api with filters`

## Purpose
Supports the "Listings" table within the Admin Dashboard. This endpoint aggregates property data and formats specific display fields (like `#LST-1234` for property codes and `OWN-5678` for owner IDs) to match the UI requirements exactly.

## Logic & Filtering
- **Dynamic Search:** The custom `@Query` performs a case-insensitive search across `title`, `location`, and the nested `seller.fullName`.
- **Dropdown Filters:** Fully supports conditional filters for `status`, `type`, and `city`. Fallback ignores values like "All Status" or "All Types" sent from the frontend.
- **DTO Mapping:** Transforms raw internal data into a flat, UI-ready payload (e.g., extracting owner name and formatting custom reference IDs).

## Endpoint & Security
- **Path:** `GET /api/v1/admin/properties`
- **Authorization:** Requires a valid JWT and is strictly restricted to `ADMIN`.

## Testing Checklist
- [ ] Make a GET request as `ADMIN` without parameters and verify default pagination (page 1, size 10).
- [ ] Apply `?status=PENDING` and verify only pending properties are returned.
- [ ] Verify `property_code` and `owner_id` are formatted correctly in the JSON response.
- [ ] Send `?search=sokun` and verify properties owned by "Sokun" or located in a matching area are returned.