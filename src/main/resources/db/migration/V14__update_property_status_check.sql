
ALTER TABLE properties DROP CONSTRAINT IF EXISTS chk_properties_status;

ALTER TABLE properties ADD CONSTRAINT chk_properties_status
    CHECK (status IN ('DRAFT', 'PENDING', 'ACTIVE', 'INACTIVE', 'REJECTED', 'SOLD_RENTED', 'BANNED'));