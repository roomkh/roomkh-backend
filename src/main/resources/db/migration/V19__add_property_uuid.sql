ALTER TABLE properties ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE properties ADD CONSTRAINT properties_uuid_unique UNIQUE (uuid);
