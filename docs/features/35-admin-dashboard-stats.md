# Feature: Admin Dashboard Stats API

- **Status:** Completed
- **Branch:** `feature/admin-dashboard-stats`
- **Related Commit:** `feat: add admin dashboard stats api`

## Purpose
Provides critical high-level metrics for the Admin Dashboard overview cards. This allows administrators to quickly gauge platform health, user distribution, and pending workloads (like listings awaiting approval).

## Logic & Aggregation
- **Total Users:** Counts accounts specifically assigned the `USER` role.
- **Total Owners:** Counts accounts specifically assigned the `SELLER` role.
- **Total Listings:** Retrieves a complete count of all properties across the platform regardless of status.
- **Pending Listings:** Filters the property count strictly by `PENDING` status to indicate moderation backlog.
- **Monthly Revenue:** Currently returns a mocked static value (`42850.00`) to fulfill frontend layout requirements until the payment/subscription module is introduced.

## Endpoint & Security
- **Path:** `GET /api/v1/admin/dashboard/stats`
- **Authorization:** Requires a valid JWT and is strictly limited to the `ADMIN` role via method-level security (`@PreAuthorize`).

## Testing Checklist
- [ ] Authenticate as a standard `USER` or `SELLER` and attempt to access the endpoint -> Verify `403 Forbidden`.
- [ ] Authenticate as an `ADMIN` and call the endpoint -> Verify `200 OK`.
- [ ] Verify the response body contains accurate aggregate counts based on the current database state.
- [ ] Verify the mocked `monthly_revenue` field is present and formatted as a float/double.