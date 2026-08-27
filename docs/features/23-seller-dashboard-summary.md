# Feature: Seller Dashboard Summary API

- **Status:** Completed
- **Branch:** `feature/seller-dashboard-summary`
- **Related Commit:** `feat: add seller dashboard summary api`

## Purpose
Provides an authenticated seller with a high-level summary of their property portfolio[cite: 1, 2]. This data hydrates the seller dashboard UI, displaying total listings, breakdowns by property status, and aggregated engagement metrics (views and inquiries)[cite: 1, 2].

## Endpoint & Authorization
- **Path:** `GET /api/v1/seller/dashboard`
- **Authorization:** `Bearer {JWT}`
- **Role Requirement:** `SELLER`

## Key Business Rules
1. **Ownership Isolation:** All counts and summations are strictly scoped to `seller.id = authenticatedUserId`.
2. **Aggregation Safety:** The `SUM` operations in JPQL use `COALESCE` to ensure a default value of `0` is returned instead of `null` if the seller has no properties or no views/inquiries yet[cite: 1, 2].
3. **Read-Only Transaction:** The service method utilizes `@Transactional(readOnly = true)` to optimize the multiple aggregation queries executed against the database[cite: 1, 2].

## Testing Checklist
- [ ] SELLER requests dashboard -> 200 OK with correct aggregated metrics
- [ ] Ensure `total_views` and `total_inquiries` return `0` for a new seller with no properties
- [ ] Ensure `total_properties` matches the sum of all status counts
- [ ] USER role access -> 403 Forbidden
- [ ] ADMIN role access -> 403 Forbidden
- [ ] Request without JWT -> 401 Unauthorized