# Feature: Admin Users Export to Excel API

- **Status:** Completed
- **Branch:** `feature/admin-users-export`
- **Related Commit:** `feat: implement user export to excel via Apache POI`

## Purpose
Enables administrators to download a comprehensive Excel (`.xlsx`) report of all users registered on the platform for offline management and reporting.

## Logic & Implementation
- **Data Retrieval:** Uses `@Transactional(readOnly = true)` to avoid `LazyInitializationException` and fetches all users sorted descending by creation date.
- **Excel Generation:** Creates an `XSSFWorkbook` using Apache POI. The User ID is formatted to match the UI representation (e.g., `USR-{id}`). Handles null values gracefully to prevent runtime crashes.
- **Stream Output:** Writes the workbook data to a `ByteArrayOutputStream` managed within a try-with-resources block for safe execution.

## Endpoint & Security
- **Path:** `GET /api/v1/admin/dashboard/users/export`
- **Authorization:** Requires valid JWT and `ADMIN` role.
- **Headers:** Sets `Content-Disposition: attachment; filename="users_export.xlsx"` and `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

## Testing Checklist
- [ ] Authenticate as an Admin.
- [ ] Trigger the `GET /dashboard/users/export` endpoint.
- [ ] Verify the response downloads a file named `users_export.xlsx`.
- [ ] Open the file to confirm the column headers (User ID, Name, Email, Phone, Role, Status, Joined Date) and data formats.