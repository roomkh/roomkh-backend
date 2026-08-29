# Feature: Admin Users List API

- **Status:** Completed
- **Branch:** `feature/admin-users-list`
- **Related Commit:** `feat: add admin paginated users list api with filters`

## Purpose
Provides the data necessary to populate the comprehensive User Management table in the Admin Dashboard. It empowers administrators to view all registered users and agents on the platform, fully supporting scalable pagination, free-text searching, and role-based filtering.

## Logic & Filtering
- **Dynamic Search:** The repository uses a custom `@Query` to perform case-insensitive `LIKE` matching across multiple fields simultaneously: `firstName`, `lastName`, `email`, and `phoneNumber`.
- **Role Filtering:** Matches precisely against the `RoleName` enum via the nested relationship (`u.role.name`), adapting to the platform's standard Spring Security implementation.
- **Pagination & Sorting:** Data is requested sequentially using Spring Data `Pageable` and consistently ordered by `createdAt DESC` to ensure newest users appear first.
- **Mapping:** Condenses full User entities into lightweight `AdminUserListItemResponse` records to protect underlying internal properties.

## Endpoint & Security
- **Path:** `GET /api/v1/admin/users`
- **Authorization:** Tightly secured; requires a valid JWT where the principal's authority claims must include `ROLE_ADMIN`.

## Testing Checklist
- [ ] Attempt execution as an unprivileged `USER` -> Expect `403 Forbidden`.
- [ ] Fetch the list as `ADMIN` with no query parameters -> Expect a page of 10 latest accounts.
- [ ] Apply `?search=sok` -> Expect results filtered exclusively to users matching "sok" in their name or email.
- [ ] Apply `?role=SELLER` -> Expect the list to restrict only to Seller/Owner roles.