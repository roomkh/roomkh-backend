CREATE TABLE properties (
                            id BIGSERIAL PRIMARY KEY,
                            seller_id BIGINT NOT NULL,
                            title VARCHAR(255) NOT NULL,
                            slug VARCHAR(300) NOT NULL UNIQUE,
                            purpose VARCHAR(20) NOT NULL,
                            property_type VARCHAR(30) NOT NULL,
                            price NUMERIC(14,2) NOT NULL,
                            currency VARCHAR(3) NOT NULL DEFAULT 'USD',
                            price_unit VARCHAR(30) NOT NULL,
                            status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                            description TEXT NOT NULL,
                            bedrooms INTEGER,
                            bathrooms INTEGER,
                            size_sqm NUMERIC(10,2),
                            floor INTEGER,
                            furnished BOOLEAN NOT NULL DEFAULT FALSE,
                            age_years INTEGER,
                            address VARCHAR(500),
                            province VARCHAR(150) NOT NULL,
                            district VARCHAR(150) NOT NULL,
                            commune VARCHAR(150) NOT NULL,
                            latitude NUMERIC(10,7),
                            longitude NUMERIC(10,7),
                            is_featured BOOLEAN NOT NULL DEFAULT FALSE,
                            view_count BIGINT NOT NULL DEFAULT 0,
                            inquiry_count BIGINT NOT NULL DEFAULT 0,
                            rejection_reason TEXT,
                            reviewed_at TIMESTAMP WITH TIME ZONE,
                            reviewed_by BIGINT,
                            listed_at TIMESTAMP WITH TIME ZONE,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_properties_seller FOREIGN KEY (seller_id) REFERENCES users (id),
                            CONSTRAINT fk_properties_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
                            CONSTRAINT chk_properties_purpose CHECK (purpose IN ('RENT', 'SALE')),
                            CONSTRAINT chk_properties_property_type CHECK (property_type IN ('ROOM', 'HOME', 'APARTMENT', 'VILLA', 'LAND')),
                            CONSTRAINT chk_properties_price_unit CHECK (price_unit IN ('MONTH', 'SELL')),
                            CONSTRAINT chk_properties_status CHECK (status IN ('DRAFT', 'PENDING', 'ACTIVE', 'REJECTED', 'SOLD_RENTED')),
                            CONSTRAINT chk_properties_purpose_price_unit_match CHECK (
                                (purpose = 'RENT' AND price_unit = 'MONTH') OR
                                (purpose = 'SALE' AND price_unit = 'SELL')
                                ),
                            CONSTRAINT chk_properties_price_positive CHECK (price > 0),
                            CONSTRAINT chk_properties_bedrooms_non_negative CHECK (bedrooms IS NULL OR bedrooms >= 0),
                            CONSTRAINT chk_properties_bathrooms_non_negative CHECK (bathrooms IS NULL OR bathrooms >= 0),
                            CONSTRAINT chk_properties_size_sqm_positive CHECK (size_sqm IS NULL OR size_sqm > 0),
                            CONSTRAINT chk_properties_floor_non_negative CHECK (floor IS NULL OR floor >= 0),
                            CONSTRAINT chk_properties_age_years_non_negative CHECK (age_years IS NULL OR age_years >= 0),
                            CONSTRAINT chk_properties_view_count_non_negative CHECK (view_count >= 0),
                            CONSTRAINT chk_properties_inquiry_count_non_negative CHECK (inquiry_count >= 0),
                            CONSTRAINT chk_properties_lat_lng_both_or_neither CHECK (
                                (latitude IS NULL AND longitude IS NULL) OR
                                (latitude IS NOT NULL AND longitude IS NOT NULL)
                                ),
                            CONSTRAINT chk_properties_latitude_range CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
                            CONSTRAINT chk_properties_longitude_range CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

CREATE INDEX idx_properties_seller_id ON properties (seller_id);
CREATE INDEX idx_properties_status ON properties (status);
CREATE INDEX idx_properties_purpose ON properties (purpose);
CREATE INDEX idx_properties_property_type ON properties (property_type);
CREATE INDEX idx_properties_province ON properties (province);
CREATE INDEX idx_properties_district ON properties (district);
CREATE INDEX idx_properties_commune ON properties (commune);
CREATE INDEX idx_properties_price ON properties (price);
CREATE INDEX idx_properties_created_at ON properties (created_at);
CREATE INDEX idx_properties_listed_at ON properties (listed_at);
CREATE INDEX idx_properties_is_featured ON properties (is_featured);

CREATE TABLE amenities (
                           id BIGSERIAL PRIMARY KEY,
                           code VARCHAR(100) NOT NULL UNIQUE,
                           name VARCHAR(150) NOT NULL,
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE property_amenities (
                                    property_id BIGINT NOT NULL,
                                    amenity_id BIGINT NOT NULL,
                                    PRIMARY KEY (property_id, amenity_id),
                                    CONSTRAINT fk_property_amenities_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE,
                                    CONSTRAINT fk_property_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id) ON DELETE CASCADE
);

CREATE INDEX idx_property_amenities_amenity_id ON property_amenities (amenity_id);