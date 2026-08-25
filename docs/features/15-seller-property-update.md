# docs/features/15-seller-property-update.md

# Feature: Seller Property Update API

**Status:** Completed (pending final test verification)

**Branch:** feature/seller-property-update

## Purpose

Allow a SELLER to fix or refine a property before it goes to admin review, while strictly preventing edits once it has entered the review/live pipeline.

## Endpoint and Authorization

`PUT /api/v1/seller/properties/{propertyId}` — requires a valid JWT with the SELLER role. USER and ADMIN receive 403; missing/invalid tokens receive 401.

## Ownership Security Rule

The property is looked up using both the path `propertyId` and the authenticated seller's ID in one query. If the property doesn't exist, or exists but belongs to a different seller, the response is identically `404 "Property not found."` in both cases — never revealing whether a property exists under someone else's account.

## Editable Status Rules

| Status | Editable? |
|---|---|
| DRAFT | Yes |
| REJECTED | Yes (so the seller can fix issues before resubmitting) |
| PENDING | No — 409 |
| ACTIVE | No — 409 |
| SOLD_RENTED | No — 409 |

## Non-Editable Status Rule

Any update attempt on a PENDING, ACTIVE, or SOLD_RENTED property returns `409 Conflict` with `"Only DRAFT or REJECTED properties can be updated."` before any field is touched.

## Request Fields and Validation

Same field set and validation rules as property creation (title, type, purpose, price, price_unit, location, description, property details), reusing the exact same `ValidPropertyPriceUnit` and `ValidCoordinates` cross-field validators. The one difference: `amenity_codes` is **required** here (though it may be an empty array), since updating a property always fully replaces its amenity list.

## Amenity Replacement Behavior

Submitting `amenity_codes` always replaces the property's entire amenity set — an empty array removes all amenities. Codes are normalized, checked for blanks/duplicates, and validated against the `amenities` table exactly as in property creation.

## Slug Update Behavior

If `title` is unchanged, the existing slug is kept as-is. If `title` changes, a brand-new unique slug is generated server-side via the same `SlugGenerator` used at creation — the client can never set a slug directly.

## Immutable Fields

This endpoint never modifies: `seller`, `status`, `isFeatured`, `viewCount`, `inquiryCount`, `reviewedAt`, `reviewedBy`, `listedAt`, `rejectionReason`, or `createdAt`. The optional `status` field in the request is only checked to confirm it matches the property's *current* status (if provided at all) — any attempt to set a different status value is rejected with 400.

## Response Format

Returns `200 OK` with the same `SellerPropertyResponse` shape used for property creation — public-safe fields only, no seller PII, tokens, review metadata, or image data.

## Explicit Out-of-Scope Features

Not built in this step: property delete, submit-for-review, status change endpoints, admin property review, public property APIs, images, Cloudinary, locations API, map integration, favorites, inquiries, seller dashboard statistics, and boost listing.

## Testing Checklist

- [ ] SELLER updates their own DRAFT property successfully
- [ ] SELLER updates their own REJECTED property successfully, status stays REJECTED
- [ ] Empty amenity_codes removes all amenities
- [ ] Changed title generates a new slug; unchanged title keeps the old slug
- [ ] Updating another seller's property returns 404
- [ ] USER and ADMIN receive 403
- [ ] Missing JWT returns 401
- [ ] Invalid purpose/price_unit combination returns 400
- [ ] Invalid coordinates return 400
- [ ] Unknown or duplicate amenity codes return 400
- [ ] PENDING, ACTIVE, and SOLD_RENTED properties all return 409
- [ ] Attempting to set status to a different value than current is rejected

## Related Commit Message
