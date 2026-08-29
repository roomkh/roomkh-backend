# Feature: Admin Update User Status API

- **Status:** Completed
- **Branch:** `feature/admin-update-user-status`
- **Related Commit:** `feat: add admin api to update user and seller status`

## Purpose
Provides the backend mechanism for administrators to moderate users on the platform. Admins can approve or reject pending agent/owner accounts, and can also ban or activate standard user accounts.

## Logic & Flow
- **Input:** Accepts a specific `action` string (`ACTIVATE`, `INACTIVE`, `BAN`, `APPROVE_SELLER`, `REJECT_SELLER`).
- **Account Actions:** Modifies the `AccountStatus` enum (applies to all users).
- **Seller Actions:** Modifies the `SellerStatus` enum. The system includes a safeguard to reject seller actions if the target user is not actually registered as a seller (i.e., `sellerStatus` is null).
- **Persistence:** Saved directly via `UserRepository` within a `@Transactional` context.

## Endpoint & Security
- **Path:** `PATCH /api/v1/admin/users/{userId}/status`
- **Authorization:** Tightly secured; requires a valid JWT with `ROLE_ADMIN`.

## Testing Checklist
- [ ] Attempt to approve a seller (`APPROVE_SELLER`). Check DB to ensure `seller_status` is updated.
- [ ] Attempt to ban a user (`BAN`). Check DB to ensure `account_status` is updated to `BANNED`.
- [ ] Attempt to send an invalid action string (e.g., `DELETE`). Verify a `400 Bad Request` or `IllegalArgumentException` is returned.
- [ ] Attempt to send `APPROVE_SELLER` for a standard USER account. Verify an error is thrown.