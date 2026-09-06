-- Allow properties to be listed as a tourism area (តំបន់ទេសចរណ៍).
ALTER TABLE properties DROP CONSTRAINT chk_properties_property_type;

ALTER TABLE properties
    ADD CONSTRAINT chk_properties_property_type
        CHECK (property_type IN ('ROOM', 'HOME', 'APARTMENT', 'VILLA', 'LAND', 'TOURISM'));
