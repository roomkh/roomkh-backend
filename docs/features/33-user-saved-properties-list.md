# Feature: Get Saved Properties List API

- **Status:** Completed
- **Branch:** `feature/user-saved-properties-list`
- **Related Commit:** `feat: add user saved properties list api`

## Purpose
Provides an endpoint for authenticated users to view their "Wishlist" or saved properties. It leverages pagination to efficiently handle users who may have saved dozens or hundreds of listings.

## Logic & Performance
- **Sorting:** Properties are ordered by `savedAt DESC`, so the most recently saved properties appear at the top.
- **Optimization:** Extracts property IDs from the paginated result and fetches their respective cover images in a single bulk query, strictly preventing N+1 database issues.
- **Reusability:** Maps the output directly to the existing `PublicPropertyListItemResponse` DTO, ensuring the frontend can flawlessly reuse the exact same Property Card components used on the home page or search page.

## Endpoint & Security
- **Path:** `GET /api/v1/user/saved-properties`
- **Authorization:** Requires a valid JWT (Any Role).

## Testing Checklist
- [ ] Ensure requesting without a JWT throws a `401 Unauthorized`.
- [ ] Save a few properties using the toggle API, then call this GET endpoint.
- [ ] Verify the returned JSON is paginated properly (`total_elements`, `total_pages`).
- [ ] Verify the properties are ordered with the most recently saved listing appearing first.
- [ ] Check SQL logs to confirm bulk fetching of cover images is working (no N+1).