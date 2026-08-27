# Feature: Seller Property Deletion API

- **Status:** Completed
- **Branch:** `feature/seller-property-deletion`
- **Related Commit:** `feat: add seller property deletion api`

## Purpose
Allows a verified seller to permanently delete their own property listing, along with all associated physical image files.

## Endpoint & Authorization
- **Path:** `DELETE /api/v1/seller/properties/{propertyId}`
- **Authorization:** `Bearer {JWT}`
- **Role Requirement:** `SELLER`

## Key Business Rules
1. **Ownership Validation:** The property must exist and belong to the authenticated seller (`owner_id = seller_id`). An HTTP 404 is returned if it does not.
2. **State Machine Deletion Rule:**
    - Allowed statuses: `DRAFT`, `REJECTED`.
    - Rejected statuses: `PENDING`, `ACTIVE`, `SOLD_RENTED` (Returns HTTP 400).
3. **Physical File Cleanup:** Before deleting the database record, the service queries all associated `PropertyImage` records and calls `PropertyImageStorage.delete()` to ensure no orphaned files remain on the server or cloud storage.
4. **Concurrency Control:** Utilizes `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`findByIdAndSellerIdForUpdate`) to lock the database row, ensuring the property cannot be submitted or updated by another concurrent thread while deletion is in progress.

## Testing Checklist
- [ ] SELLER deletes their own DRAFT property -> 200 OK
- [ ] SELLER deletes their own REJECTED property -> 200 OK
- [ ] Physical image files are confirmed removed from storage after successful deletion
- [ ] SELLER attempts to delete a PENDING property -> 400 Bad Request
- [ ] SELLER attempts to delete an ACTIVE property -> 400 Bad Request
- [ ] SELLER attempts to delete another seller's property -> 404 Not Found
- [ ] USER role access -> 403 Forbidden
- [ ] ADMIN role access -> 403 Forbidden
- [ ] Request without JWT -> 401 Unauthorized