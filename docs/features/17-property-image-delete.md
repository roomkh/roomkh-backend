# Feature: Property Image Delete API

**Status:** Completed (pending final test verification)

**Branch:** feature/property-image-delete

## Purpose

Allow authenticated SELLER users to delete image records and stored image files from their own editable properties while keeping cover-image rules consistent.

## Endpoint and Authorization

`DELETE /api/v1/seller/properties/{propertyId}/images/{imageId}`

The endpoint requires a valid JWT with the SELLER role.

- USER receives `403 Forbidden`
- ADMIN receives `403 Forbidden`
- Missing or invalid JWT receives `401 Unauthorized`

## Ownership Rule

The backend finds the property using both:

- `propertyId` from the URL
- Authenticated seller ID from the JWT security context

If the property is missing or belongs to another seller, the API returns:

```json
{
  "success": false,
  "message": "Property not found.",
  "data": null
}
```

The image is then found using both `imageId` and `propertyId`, preventing deletion of an image belonging to another property.

## Editable Property Status Rule

Images can only be deleted from properties with:

- `DRAFT`
- `REJECTED`

Deletion is rejected for:

- `PENDING`
- `ACTIVE`
- `SOLD_RENTED`

Response:

```json
{
  "success": false,
  "message": "Only DRAFT or REJECTED properties can be modified.",
  "data": null
}
```

## Cover Image Deletion Behavior

When deleting a non-cover image:

- Only that image is deleted.
- The current cover remains unchanged.

When deleting the cover image:

1. The selected cover image metadata is deleted.
2. The remaining image with the lowest `sort_order` becomes the new cover.
3. If no images remain, the property has no cover image.
4. Sort order values are not renumbered.

## Database Deletion Behavior

The `property_images` metadata record is deleted inside a database transaction.

- The target property row is locked with `PESSIMISTIC_WRITE`.
- This prevents concurrent image operations from causing inconsistent cover assignments.
- The partial unique database index still guarantees at most one cover image per property.

## Local Storage Deletion Behavior

The storage key is read only from the stored `PropertyImage` database record.

The API never accepts a filesystem path or storage key from the client.

After the metadata deletion transaction commits, the backend attempts to delete the physical image through `PropertyImageStorage`.

If the file is already missing, cleanup continues normally.

If physical deletion fails after database commit:

- The API does not expose the filesystem path.
- The metadata record is not recreated.
- A secure server-side warning is logged.
- An orphaned file may require operational cleanup.

## Transaction and After-Commit Handling

Physical storage deletion runs after transaction commit.

This design prevents a database rollback from deleting a file that still has a valid metadata record.

The database remains the source of truth. In rare storage cleanup failures, an orphan file is safer than returning the system to an inconsistent state where metadata points to a deleted file after rollback.

## Security Protections

- Seller ownership is verified at database-query level.
- Other sellers cannot discover or delete images they do not own.
- The property row is locked during deletion and cover reassignment.
- Client input never controls storage keys or physical paths.
- Internal storage details are never returned in the response.

## Testing Checklist

- [ ] SELLER deletes own non-cover image
- [ ] SELLER deletes own cover image with remaining images
- [ ] Lowest sort-order remaining image becomes cover
- [ ] Deleting the final cover image leaves no cover image
- [ ] Remaining sort orders are unchanged
- [ ] Another seller cannot delete an image
- [ ] USER and ADMIN receive 403
- [ ] Missing JWT receives 401
- [ ] PENDING and ACTIVE properties reject deletion with 409
- [ ] Missing image returns 404
- [ ] Image under a different property ID returns 404

## Explicit Out-of-Scope Features

Not implemented in this step:

- Image reordering
- Cloudinary integration
- External image URLs
- Property deletion
- Property submission for review
- Admin property review
- Public property APIs

## Related Commit Message

```text
feat: add property image delete api
```