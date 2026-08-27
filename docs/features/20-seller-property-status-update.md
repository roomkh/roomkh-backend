# Feature: Seller Property Status Update API

- **Status:** Completed
- **Branch:** `feature/seller-property-status-update`
- **Related Commit:** `feat: add seller property status update api`

## Purpose
Allows a verified seller to update the status of their own property. The primary use case is marking a currently `ACTIVE` property as `SOLD_RENTED` to indicate it is no longer available on the public market.

## Endpoint & Authorization
- **Path:** `PATCH /api/v1/seller/properties/{propertyId}/status`
- **Authorization:** `Bearer {JWT}`
- **Role Requirement:** `SELLER`

## Key Business Rules
1. **Ownership Validation:** The property must exist and belong to the authenticated seller (`owner_id = seller_id`). An HTTP 404 is returned if it does not, protecting against data leakage.
2. **Payload Validation:** The `status` field in the request body is required and must exactly equal `SOLD_RENTED`.
3. **State Machine Rule:**
    - Allowed transition: `ACTIVE` $\rightarrow$ `SOLD_RENTED`.
    - Rejections: `DRAFT`, `REJECTED`, or `PENDING` $\rightarrow$ `SOLD_RENTED` (Returns HTTP 400).
    - Rejections: Already `SOLD_RENTED` (Returns HTTP 400).
4. **Concurrency Control:** Utilizes `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findByIdAndSellerIdForUpdate`) to lock the database row during the transaction, preventing race conditions from simultaneous requests.

## Testing Checklist
- [ ] SELLER marks ACTIVE property as SOLD_RENTED -> 200 OK
- [ ] SELLER attempts to mark DRAFT property as SOLD_RENTED -> 400 Bad Request
- [ ] SELLER attempts to mark PENDING property as SOLD_RENTED -> 400 Bad Request
- [ ] SELLER attempts to mark already SOLD_RENTED property -> 400 Bad Request
- [ ] SELLER submits invalid status (e.g., "ACTIVE") in payload -> 400 Bad Request
- [ ] SELLER attempts to update another seller's property -> 404 Not Found
- [ ] USER role access -> 403 Forbidden
- [ ] ADMIN role access -> 403 Forbidden
- [ ] Request without JWT -> 401 Unauthorized