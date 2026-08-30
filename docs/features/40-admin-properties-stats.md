# Feature: Admin Properties Statistics API

- **Status:** Completed
- **Branch:** `feature/admin-properties-stats`
- **Related Commit:** `feat: add admin properties stats api with date range and trends`

## Purpose
Supplies data for the top KPI cards on the Admin Property Listings dashboard. It tracks the volume of total, active, pending, and inactive properties over a given date range and provides a percentage comparison against the previous identical period.

## Logic & Math
- **Date Handling:** Accepts `startDate` and `endDate` via request parameters. If missing, it defaults to the last 30 days. It converts `LocalDate` (UI input) to `OffsetDateTime` (start/end of day) for accurate database querying.
- **Previous Period Calculation:** It mathematically counts the duration (in days) between the requested start and end date, then steps backward by that exact duration to establish the previous period.
- **Trend Calculation Formula:** `((current - previous) / previous) * 100.0`. Handled edge cases where the previous count is `0`. The result is rounded to one decimal place (e.g., `14.2`, `-6.3`).

## Endpoint & Security
- **Path:** `GET /api/v1/admin/properties/stats`
- **Authorization:** Requires valid JWT, role must be `ADMIN`.

## Testing Checklist
- [ ] Call the endpoint with no parameters and verify it defaults to a 30-day window.
- [ ] Call the endpoint providing `startDate` and `endDate` parameters (e.g., `?startDate=2026-08-01&endDate=2026-08-31`).
- [ ] Verify that trend percentages correctly output positive or negative values bounded to one decimal place.