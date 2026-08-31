# Feature: Admin Properties Export to Excel API

- **Status:** Completed
- **Branch:** `feature/admin-property-export`
- **Related Commit:** `feat: implement property export to excel via Apache POI`

## Purpose
Enables administrators to download a comprehensive Excel (`.xlsx`) report of all properties currently in the system for external reporting and analytics.

## Logic & Implementation
- **Dependency:** Integrated `org.apache.poi:poi-ooxml` to generate valid `.xlsx` documents.
- **Service Action:** Fetches all properties sorted descending by creation date. It maps the entities to a standard `XSSFWorkbook`, dynamically concatenating nested or nullable fields (e.g., Seller Name, District + Province).
- **Stream Handling:** Writes the completed workbook into a `ByteArrayOutputStream` wrapped in a try-with-resources block for automatic memory management and safe closure, throwing a `RuntimeException` if I/O fails.

## Endpoint & Security
- **Path:** `GET /api/v1/admin/properties/export`
- **Authorization:** Requires valid JWT and `ADMIN` role.
- **Headers Returned:** Sets standard file download headers (`Content-Disposition: attachment; filename="properties_export.xlsx"`) and specifies the correct MIME type for Office Open XML.

## Testing Checklist
- [ ] Authenticate as an Admin and trigger the `GET /properties/export` endpoint in browser or Postman.
- [ ] Verify the response triggers a file download named `properties_export.xlsx`.
- [ ] Open the file in Excel/Google Sheets to confirm the correct population of columns (ID, Code, Title, Owner Name, Type, Location, Price, Status, Listed Date).
- [ ] Ensure rows with null relationships (e.g., no seller) gracefully fallback to "N/A" rather than throwing `NullPointerException`.