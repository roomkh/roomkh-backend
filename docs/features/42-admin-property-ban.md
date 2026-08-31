# Feature: Admin Property Soft Delete (Ban) API

- **Status:** Completed
- **Branch:** `feature/admin-property-ban`
- **Related Commit:** `feat: implement admin property soft delete via status ban`

## Purpose
Enables administrators to soft-delete properties that violate platform rules without permanently destroying database records. This maintains referential integrity (e.g., user favorites, historical inquiries) while safely removing the listing from public view.

## Logic & Implementation
- **Status Update:** Rather than adding an `is_deleted` column and managing database migrations, the `PropertyStatus` enum was expanded to include `BANNED`.
- **Service Action:** Looks up the property by ID. If found, mutates the status to `BANNED` and updates the `updatedAt` timestamp.
- **Data Integrity:** Protects the database from orphaned foreign keys by leaving the row intact while effectively filtering it out of active queries.

## Endpoint & Security
- **Path:** `DELETE /api/v1/admin/properties/{id}`
- **Authorization:** Requires valid JWT and `ADMIN` role.

## Testing Checklist
- [ ] Make a DELETE request to an existing property ID and verify the database status updates to `BANNED`.
- [ ] Attempt a DELETE request on a non-existent property and verify a 404 response.
- [ ] Ensure `updatedAt` successfully refreshes upon ban execution.