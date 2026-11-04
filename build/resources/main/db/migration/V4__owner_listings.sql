CREATE TABLE listings (
    id UUID PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(160) NOT NULL,
    purpose VARCHAR(4) NOT NULL CHECK (purpose IN ('RENT','SALE')),
    property_type_id BIGINT NOT NULL REFERENCES property_types(id),
    province_id BIGINT NOT NULL REFERENCES provinces(id),
    address VARCHAR(500) NOT NULL,
    bedrooms INTEGER NOT NULL CHECK (bedrooms BETWEEN 0 AND 100),
    bathrooms INTEGER NOT NULL CHECK (bathrooms BETWEEN 0 AND 100),
    area NUMERIC(14,2) NOT NULL CHECK (area > 0),
    price NUMERIC(14,2) NOT NULL CHECK (price > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('USD','KHR')),
    description TEXT NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    amenities VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','PENDING_REVIEW')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_listings_owner_created ON listings(owner_id, created_at DESC);
CREATE TABLE listing_photos (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    content BYTEA NOT NULL
);
CREATE INDEX idx_listing_photos_listing ON listing_photos(listing_id, position);
