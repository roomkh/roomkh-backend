# Feature: Property Image Reorder API

**Status:** Completed (pending final test verification)

**Branch:** feature/property-image-reorder

## Purpose

Allow authenticated SELLER users to completely reorder property images for their own editable properties.

## Endpoint and Authorization

```text
PATCH /api/v1/seller/properties/{propertyId}/images/order
```

The endpoint requires a valid JWT with the `SELLER` role.

- USER receives `403 Forbidden`
- ADMIN receives `403 Forbidden`
- Missing or invalid JWT receives `401 Unauthorized`

## Ownership Rule

The backend loads the property using:

- `propertyId` from the URL
- Seller ID from the authenticated JWT context

A property belonging to another seller returns the same response as a missing property:

```json
{
  "success": false,
  "message": "Property not found.",
  "data": null
}
```

This prevents a seller from discovering other sellers' properties.

## Editable Property Status Rule

Images can only be reordered while the property is:

- `DRAFT`
- `REJECTED`

The endpoint rejects reordering for:

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

## Request Format

```json
{
  "image_ids":[1][2][3][4]
}
```

Rules:

- `image_ids` is required.
- It must not be empty.
- It can contain at most 10 IDs.
- Every ID must be positive.
- IDs must not repeat.
- The request must contain every existing image for the property exactly once.

Partial reorders are not permitted.

## Sequential Sort-Order Rule

The backend replaces all existing sort orders with a sequential order starting at 1.

| Submitted Position | Resulting sort_order |
|---:|---:|
| First | 1 |
| Second | 2 |
| Third | 3 |
| Next images | Continue sequentially |

## Cover Image Assignment Rule

The first image ID in `image_ids` always becomes the cover image:

- First image: `is_cover = true`
- All remaining images: `is_cover = false`

After a successful reorder, exactly one property image is the cover image.

## Two-Phase Sort-Order Strategy

Some database configurations can enforce uniqueness on property image sort-order values. Reordering directly can briefly create duplicate sort orders during updates.

To prevent this, the service uses one transaction with two update phases:

1. Lock the target property row with `PESSIMISTIC_WRITE`.
2. Load and validate all property images.
3. Set temporary unique negative sort orders on all images.
4. Flush temporary database values.
5. Apply final sort orders from 1 onward.
6. Set the first image as cover and all other images as non-cover.
7. Commit the transaction.

## Transaction and Concurrency Safety

The target property row is locked before images are loaded.

This prevents simultaneous reorder requests from reading and updating the same property's image list at the same time. The transaction guarantees the property cannot be left with duplicate sort orders, incomplete order data, or multiple cover images.

## Testing Checklist

- [ ] SELLER reorders all own property images successfully
- [ ] First submitted image becomes the cover image
- [ ] Resulting sort orders are sequential from 1
- [ ] Duplicate image IDs return 400
- [ ] Empty image_ids returns 400
- [ ] Partial image lists return 400
- [ ] Image ID from another property returns 400
- [ ] Another seller's image cannot be reordered
- [ ] USER and ADMIN receive 403
- [ ] Missing JWT receives 401
- [ ] PENDING, ACTIVE, and SOLD_RENTED properties return 409

## Explicit Out-of-Scope Features

Not implemented in this step:

- Cloudinary integration
- External image URLs
- Property update changes
- Property deletion
- Public property APIs
- Admin property review

## Related Commit Message

```text
feat: add property image reorder api
```