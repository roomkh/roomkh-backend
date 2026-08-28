# Feature: Public Home Page Data API

- **Status:** Completed
- **Branch:** `feature/public-home-page-data`
- **Related Commit:** `feat: add public home page data api`

## Purpose
Provides a consolidated endpoint to populate the application's landing page. By aggregating both "Featured Properties" and "Browse by Location" data into a single payload, it significantly reduces frontend network overhead and improves page load performance.

## Components
1. **Featured Properties:** Returns up to 8 of the newest properties with an `ACTIVE` status. Bulk fetching logic is implemented for cover images to maintain strict O(1) query performance.
2. **Browse by Location:** Returns the top 4 provinces dynamically ranked by their total count of `ACTIVE` properties using a grouped JPQL query.

## Endpoint & Security
- **Path:** `GET /api/v1/public/home`
- **Authorization:** None (Publicly accessible).

## Testing Checklist
- [ ] Fetch the home endpoint -> 200 OK.
- [ ] Verify `featured_properties` array contains no more than 8 elements and all have `status = ACTIVE` (implied by missing unavailable properties).
- [ ] Verify `locations` array contains up to 4 objects with correctly aggregated `property_count` metrics.
- [ ] Ensure the endpoint functions properly without passing a JWT.