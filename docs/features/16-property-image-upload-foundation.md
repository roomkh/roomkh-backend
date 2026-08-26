# docs/features/16-property-image-upload-foundation.md

# Feature: Property Image Upload Foundation

**Status:** Completed (pending final test verification)

**Branch:** feature/property-image-upload-foundation

## Purpose

Let a SELLER attach real images to their own editable properties, backed by a storage abstraction that works locally today and can be swapped for a real cloud provider later without touching the API contract.

## Endpoint and Authorization

`POST /api/v1/seller/properties/{propertyId}/images` — requires a valid JWT with the SELLER role. USER and ADMIN receive 403; missing/invalid tokens receive 401.

## Editable Property Status Rule

Images can only be uploaded to properties currently in DRAFT or REJECTED status. PENDING, ACTIVE, and SOLD_RENTED properties return 409 "Only DRAFT or REJECTED properties can be modified." — image upload never changes the property's status itself.

## Multipart Request Fields

| Field | Required | Notes |
|---|---|---|
| image | Yes | The image file |
| is_cover | No | Whether this should become the cover image |
| sort_order | No | Explicit position; auto-assigned if omitted |

## Accepted Formats and Size Limit

Only `image/jpeg` and `image/png` are accepted, maximum 5 MB (5,242,880 bytes exactly).

## File Validation Approach

Validation happens in three layers: declared content type check, a hard file-size check, and genuine pixel-level decoding via Java's built-in `ImageIO` to confirm the file is a real, undamaged JPEG or PNG — not just a renamed or spoofed file. Corrupted, empty, oversized, or unsupported files are all rejected with clear, non-technical error messages, and the original filename is never used for storage — every file gets a random UUID-based name.

## Local Development Storage Design

In `dev`/`test`, images are written to a configurable local directory (`PROPERTY_IMAGE_STORAGE_PATH`, default `uploads/properties/`) outside `src/main/resources`, with path-traversal protection via normalized path validation.

## Public Development URL Serving

`/uploads/**` is served publicly via a Spring MVC resource handler only in `dev`/`test` — this exposes only the configured image directory, nothing else on the filesystem, and doesn't exist at all in production.

## Cover Image Behavior

The first image uploaded to a property always becomes the cover automatically, regardless of the `is_cover` value sent. For subsequent uploads, setting `is_cover=true` demotes the previous cover and promotes the new one, transactionally, guaranteeing exactly one cover per property (enforced by both application logic and a database partial unique index).

## Sort Order Behavior

If omitted, the new image gets the next available sort order (highest existing + 1, or 1 if none exist). If explicitly supplied, it must be positive and not already used by another image of the same property, or the request returns 409.

## Production Storage Safety

No real cloud storage exists yet. In production, `property-image-storage.provider=unavailable` means image upload attempts fail cleanly with `503 "Property image storage is not configured."` rather than silently pretending success or crashing the whole application at startup.

## Cloudinary Deferred Note

Cloudinary/S3 integration is explicitly deferred to a future step — this step only builds the storage abstraction (`PropertyImageStorage`) that a real provider implementation will plug into later without requiring any controller or database changes.

## Database Table

`property_images` stores URL, generated storage key, content type, file size, cover flag, sort order, and timestamps, linked to `properties` via `ON DELETE CASCADE` so deleting a property automatically cleans up its image records.

## Security Rules

Ownership is verified via `propertyId` + authenticated `sellerId` in one query; a non-existent property and another seller's property both return an identical 404, revealing nothing about ownership. No seller ID, storage key, or filesystem path is ever accepted from or returned to the client.

## Testing Checklist

- [ ] First uploaded image automatically becomes cover
- [ ] Non-cover image upload works when a cover already exists
- [ ] `is_cover=true` correctly demotes the previous cover
- [ ] Upload to REJECTED property succeeds; PENDING/ACTIVE/SOLD_RENTED all return 409
- [ ] Another seller's property returns 404
- [ ] Oversized, wrong-type, and corrupted files are all rejected with clear messages
- [ ] Duplicate sort_order returns 409
- [ ] Uploaded image is viewable via its returned `/uploads/...` URL in development

## Explicit Out-of-Scope Features

Not built in this step: image deletion, image reordering, Cloudinary/S3 integration, property submission for review, admin property review, and public property APIs.

## Related Commit Message
