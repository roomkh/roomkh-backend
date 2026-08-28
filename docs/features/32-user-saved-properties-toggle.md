# Feature: Toggle Save Property API

- **Status:** Completed
- **Branch:** `feature/user-saved-properties-toggle`
- **Related Commit:** `feat: add toggle save property api`

## Purpose
Allows authenticated users (of any role) to bookmark properties they are interested in. The API behaves as a toggle: calling it on an unsaved property will save it, and calling it on an already saved property will remove it.

## Entity & Database Design
- **`SavedProperty` Entity:** Serves as a mapping table between `User` and `Property`.
- **Constraint:** A unique compound index on `(user_id, property_id)` guarantees that a user cannot save the same property multiple times.

## Endpoint & Security
- **Path:** `POST /api/v1/user/saved-properties/{propertyId}/toggle`
- **Authorization:** Requires a valid JWT.

## Visibility Logic
- The system enforces a strict `ACTIVE` check on the target property. A user cannot save a property that is `DRAFT`, `PENDING`, or `SOLD_RENTED`. Attempting to do so returns a `404 Not Found`.

## Testing Checklist
- [ ] Call the endpoint without a JWT -> 401 Unauthorized.
- [ ] Call with JWT on an `ACTIVE` property for the first time -> 200 OK (`is_saved: true`).
- [ ] Check the database to verify the `saved_properties` record was created.
- [ ] Call the endpoint again on the same property -> 200 OK (`is_saved: false`).
- [ ] Check the database to verify the record was deleted.
- [ ] Attempt to call the endpoint on a `DRAFT` or `PENDING` property -> 404 Not Found.