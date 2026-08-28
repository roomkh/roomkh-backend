# Feature: Public Property Detail API

- **Status:** Completed
- **Branch:** `feature/public-property-detail`
- **Related Commit:** `feat: add public property detail api`

## Purpose
Provides the complete details of a single property for the frontend Property Detail Page. This includes physical attributes, location details, an ordered list of images, amenity codes, and basic seller contact information so interested buyers/renters can reach out.

## Visibility Rule (Critical)
The endpoint strictly enforces a state machine visibility rule: it only queries for `status = ACTIVE`. If a user attempts to access a property that is `DRAFT`, `PENDING`, `REJECTED`, or `SOLD_RENTED`, the system returns a standard `404 Not Found` to prevent data leakage regarding the property's actual internal state.

## Side Effects
- **View Count Increment:** Calling this endpoint successfully triggers an automatic increment of the property's `view_count` by 1. The transaction is fully flushed to the database to persist this engagement metric.

## Testing Checklist
- [ ] Fetch an `ACTIVE` property -> 200 OK.
- [ ] Verify the response includes `seller` contact info, `images`, and `amenity_codes`.
- [ ] Verify the `view_count` increments by 1 in the database after a successful request.
- [ ] Fetch a `PENDING` or `DRAFT` property -> 404 Not Found.
- [ ] Ensure the API is fully accessible without passing a JWT.