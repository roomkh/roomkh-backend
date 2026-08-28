# Feature: Public Property Contact Click API

- **Status:** Completed
- **Branch:** `feature/public-property-contact-click`
- **Related Commit:** `feat: add public property contact click api`

## Purpose
Provides a lightweight endpoint to track user engagement with property listings. When a user clicks a "Contact Seller" button (like Telegram, WhatsApp, or Call) on the frontend, this endpoint increments the property's `inquiry_count` to populate the seller's dashboard analytics.

## Endpoint & Security
- **Path:** `POST /api/v1/public/properties/{propertyId}/contact-clicks`
- **Authorization:** None (Publicly accessible).

## Visibility Rule (Critical)
The API strictly verifies that the target property is `ACTIVE`. Attempting to record a click for a property that is `DRAFT`, `PENDING`, `REJECTED`, or `SOLD_RENTED` will result in a `404 Not Found` response to prevent artificial inflation of metrics on unavailable listings.

## Side Effects
- **Inquiry Count Increment:** Successfully calling this endpoint increments the `inquiry_count` column of the specified `Property` entity by exactly 1.

## Testing Checklist
- [ ] Send POST request to an `ACTIVE` property -> 200 OK.
- [ ] Verify the `inquiry_count` increments by 1 in the database.
- [ ] Send POST request to a `PENDING` or `DRAFT` property -> 404 Not Found.
- [ ] Send POST request to a non-existent property -> 404 Not Found.
- [ ] Verify endpoint functions properly without passing a JWT.