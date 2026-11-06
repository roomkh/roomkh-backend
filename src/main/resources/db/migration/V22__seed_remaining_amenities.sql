-- The seller "Add property" form offers 14 amenities, but V11 only seeded 5 of
-- them, so choosing any of the others failed with "Unknown amenity code(s)".
INSERT INTO amenities (code, name, created_at, updated_at) VALUES
    ('ELEVATOR', 'Elevator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('GENERATOR', 'Generator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FULLY_FURNISHED', 'Fully Furnished', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CITY_VIEW', 'City View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BATHTUB', 'Bathtub', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('GYM', 'Gym', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('POOL', 'Swimming Pool', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CLEANING', 'Cleaning Service', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PET_FRIENDLY', 'Pet Friendly', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (code) DO NOTHING;
