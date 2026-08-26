CREATE TABLE property_images (
                                 id BIGSERIAL PRIMARY KEY,
                                 property_id BIGINT NOT NULL,
                                 url VARCHAR(1000) NOT NULL,
                                 storage_key VARCHAR(500) NOT NULL UNIQUE,
                                 content_type VARCHAR(100) NOT NULL,
                                 file_size BIGINT NOT NULL,
                                 is_cover BOOLEAN NOT NULL DEFAULT FALSE,
                                 sort_order INTEGER NOT NULL,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE,
                                 CONSTRAINT chk_property_images_content_type CHECK (content_type IN ('image/jpeg', 'image/png')),
                                 CONSTRAINT chk_property_images_file_size_positive CHECK (file_size > 0),
                                 CONSTRAINT chk_property_images_file_size_max CHECK (file_size <= 5242880),
                                 CONSTRAINT chk_property_images_sort_order_positive CHECK (sort_order > 0)
);

CREATE INDEX idx_property_images_property_id ON property_images (property_id);
CREATE INDEX idx_property_images_property_sort_order ON property_images (property_id, sort_order);
CREATE INDEX idx_property_images_property_is_cover ON property_images (property_id, is_cover);

CREATE UNIQUE INDEX uq_property_images_one_cover_per_property
    ON property_images (property_id)
    WHERE is_cover = TRUE;