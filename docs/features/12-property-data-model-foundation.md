# docs/features/12-property-data-model-foundation.md

# Feature: Property Data Model Foundation

**Status:** Completed (pending final migration verification)

**Branch:** feature/property-data-model-foundation

## Purpose

Establish the PostgreSQL schema and JPA entities required for the future Property Module — property listings and their amenities — without yet building any create, update, search, or review functionality on top of them.

## Property Ownership Relationship

Each `Property` belongs to exactly one seller via `seller_id`, a foreign key into `users.id`. The database does not restrict this to users with the `SELLER` role specifically — that validation is deferred to the future service layer, since a plain foreign key can only reference `users` generically.

## Property Status Lifecycle (Data Model Only)

| Status | Meaning |
|---|---|
| DRAFT | Seller saved the property but has not submitted it |
| PENDING | Seller submitted the property for admin review |
| ACTIVE | Admin approved the property; visible publicly |
| REJECTED | Admin rejected the property |
| SOLD_RENTED | Seller marked the property as no longer available |

No status-change logic is implemented in this step — only the enum and its allowed database values.

## Property Type Dropdown Values

| API Value | Frontend Label |
|---|---|
| ROOM | Room |
| HOME | Home |
| APARTMENT | Apartment |
| VILLA | Villa |
| LAND | Land |

`CONDO` is intentionally excluded, since it isn't currently available in the seller property form dropdown.

## Property Purpose Dropdown Values

| API Value | Frontend Label |
|---|---|
| RENT | Rent |
| SALE | Sale |

## Price and Price Unit Rules

| API Value | Frontend Label |
|---|---|
| MONTH | /month |
| SELL | Sell |

A database check constraint enforces that `RENT` properties must use `MONTH` pricing, and `SALE` properties must use `SELL` pricing — this pairing can never be violated, even by a future bug in application code.

## Province, District, Commune, and Map Coordinate Storage

`province`, `district`, and `commune` are all required text fields sourced from frontend dropdowns; no locations table or API exists yet — these are stored as free text for now. `address` is optional. `latitude` and `longitude` are optional, but a database constraint requires both to be present together or both absent — never just one. When present, latitude must fall between -90 and 90, and longitude between -180 and 180.

## Property and Amenity Relationship

A property can have many amenities, and an amenity can belong to many properties, connected through the `property_amenities` join table with no extra columns beyond the two foreign keys. No amenity seed data or management API exists yet.

## Database Tables

| Table | Purpose |
|---|---|
| `properties` | Core property listing data, ownership, pricing, location, and review tracking |
| `amenities` | Master list of amenity codes and display names |
| `property_amenities` | Many-to-many join table linking properties to amenities |

## Database Indexes

Indexes exist on `seller_id`, `status`, `purpose`, `property_type`, `province`, `district`, `commune`, `price`, `created_at`, `listed_at`, and `is_featured` to support future filtering and search without full table scans. `slug`'s `UNIQUE` constraint already provides its own index.

## Database Constraints

- `purpose`, `property_type`, `price_unit`, and `status` are each restricted to their exact allowed values.
- `purpose`/`price_unit` pairing is enforced (RENT↔MONTH, SALE↔SELL).
- `price` must be greater than 0; `size_sqm`, when present, must be greater than 0.
- `bedrooms`, `bathrooms`, `floor`, and `age_years`, when present, cannot be negative.
- `view_count` and `inquiry_count` cannot be negative.
- `latitude`/`longitude` must be both present or both null, and each must fall within valid geographic ranges.
- `property_amenities` foreign keys cascade on delete.

## Explicit Out-of-Scope Features

Not built in this step: Property CRUD APIs, image upload/Cloudinary, property submission, public property list/detail/search, admin property review, favorites, inquiries, a locations API, Google Maps/Mapbox integration, reverse geocoding, and amenity seed data.

## Testing Checklist

- [ ] Migration applies cleanly with no errors
- [ ] `properties`, `amenities`, and `property_amenities` tables exist with correct columns
- [ ] All CHECK constraints are visible via `\d properties`
- [ ] Inserting a RENT property with price_unit SELL fails as expected
- [ ] Inserting a property with only latitude (no longitude) fails as expected
- [ ] Application starts successfully with the new entities

## Related Commit Message
