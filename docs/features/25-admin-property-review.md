# Feature: Admin Approve/Reject Property API

- **Status:** Completed
- **Branch:** `feature/admin-property-review`
- **Related Commit:** `feat: add admin property review api`

## Purpose
Provides functionality for a platform administrator to review a property listing submitted by a seller. The admin can approve the property (making it `ACTIVE` and publicly visible) or reject it (`REJECTED`) while providing a mandatory reason for the seller to rectify. It includes robust auditing.

## Endpoint & Authorization
- **Path:** `PATCH /api/v1/admin/properties/{propertyId}/review`
- **Authorization:** `Bearer {JWT}`
- **Role Requirement:** `ADMIN`

## Key Business Rules
1. **Security Isolation:** Enforces strict role checks (`@PreAuthorize("hasRole('ADMIN')")`).
2. **State Machine Transitions:**
    - Property must currently be `PENDING`.
    - Allowed updates are to `ACTIVE` or `REJECTED` only.
3. **Validation Requirements:**
    - If rejecting, the `rejection_reason` field is strictly required.
    - If approving, any existing `rejection_reason` is automatically cleared to maintain clean data.
4. **Auditing & Concurrency:**
    - Locks the property row during review using `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findByIdForUpdate`).
    - Automatically injects the authenticated admin's `User` reference into `reviewedBy` and sets `reviewedAt` to the current timestamp.

## Testing Checklist
- [ ] ADMIN approves a PENDING property -> 200 OK (Status is ACTIVE, `reviewedBy` is set)
- [ ] ADMIN rejects a PENDING property with a reason -> 200 OK (Status is REJECTED)
- [ ] ADMIN attempts to reject without a reason -> 400 Bad Request
- [ ] ADMIN attempts to approve a DRAFT or ACTIVE property -> 400 Bad Request
- [ ] ADMIN submits invalid status string -> 400 Bad Request
- [ ] SELLER role access -> 403 Forbidden
- [ ] Request without JWT -> 401 Unauthorized