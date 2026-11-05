-- ============================================================================
-- V1: PostGIS/unaccent extensions + Cambodian administrative taxonomy +
--     property types + amenities.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ----------------------------------------------------------------------------
-- Administrative hierarchy: provinces -> districts -> communes -> villages
-- ----------------------------------------------------------------------------

CREATE TABLE provinces (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(16) NOT NULL,
    name_km     TEXT NOT NULL,
    name_en     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_provinces_code UNIQUE (code)
);

CREATE TABLE districts (
    id          BIGSERIAL PRIMARY KEY,
    province_id BIGINT NOT NULL REFERENCES provinces (id) ON DELETE CASCADE,
    code        VARCHAR(16) NOT NULL,
    name_km     TEXT NOT NULL,
    name_en     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_districts_code UNIQUE (code)
);
CREATE INDEX idx_districts_province_id ON districts (province_id);

CREATE TABLE communes (
    id          BIGSERIAL PRIMARY KEY,
    district_id BIGINT NOT NULL REFERENCES districts (id) ON DELETE CASCADE,
    code        VARCHAR(16) NOT NULL,
    name_km     TEXT NOT NULL,
    name_en     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_communes_code UNIQUE (code)
);
CREATE INDEX idx_communes_district_id ON communes (district_id);

CREATE TABLE villages (
    id          BIGSERIAL PRIMARY KEY,
    commune_id  BIGINT NOT NULL REFERENCES communes (id) ON DELETE CASCADE,
    code        VARCHAR(16) NOT NULL,
    name_km     TEXT NOT NULL,
    name_en     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_villages_code UNIQUE (code)
);
CREATE INDEX idx_villages_commune_id ON villages (commune_id);

-- ----------------------------------------------------------------------------
-- Property types & amenities
-- ----------------------------------------------------------------------------

CREATE TABLE property_types (
    id          BIGSERIAL PRIMARY KEY,
    key         VARCHAR(32) NOT NULL,
    name_km     TEXT NOT NULL,
    name_en     TEXT NOT NULL,
    applies_to  VARCHAR(8) NOT NULL CHECK (applies_to IN ('RENT', 'SALE', 'BOTH')),
    icon        VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_property_types_key UNIQUE (key)
);

CREATE TABLE amenities (
    id          BIGSERIAL PRIMARY KEY,
    key         VARCHAR(32) NOT NULL,
    name_km     TEXT NOT NULL,
    name_en     TEXT NOT NULL,
    icon        VARCHAR(64),
    category    VARCHAR(32),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_amenities_key UNIQUE (key)
);

-- ----------------------------------------------------------------------------
-- Seed: all 25 Cambodian provinces (MoI 2-digit codes, km + en names).
-- ----------------------------------------------------------------------------

INSERT INTO provinces (code, name_km, name_en) VALUES
    ('01', 'បន្ទាយមានជ័យ', 'Banteay Meanchey'),
    ('02', 'បាត់ដំបង', 'Battambang'),
    ('03', 'កំពង់ចាម', 'Kampong Cham'),
    ('04', 'កំពង់ឆ្នាំង', 'Kampong Chhnang'),
    ('05', 'កំពង់ស្ពឺ', 'Kampong Speu'),
    ('06', 'កំពង់ធំ', 'Kampong Thom'),
    ('07', 'កំពត', 'Kampot'),
    ('08', 'កណ្ដាល', 'Kandal'),
    ('09', 'កោះកុង', 'Koh Kong'),
    ('10', 'ក្រចេះ', 'Kratie'),
    ('11', 'មណ្ឌលគិរី', 'Mondulkiri'),
    ('12', 'ភ្នំពេញ', 'Phnom Penh'),
    ('13', 'ព្រះវិហារ', 'Preah Vihear'),
    ('14', 'ព្រៃវែង', 'Prey Veng'),
    ('15', 'ពោធិ៍សាត់', 'Pursat'),
    ('16', 'រតនគិរី', 'Ratanakiri'),
    ('17', 'សៀមរាប', 'Siem Reap'),
    ('18', 'ព្រះសីហនុ', 'Preah Sihanouk'),
    ('19', 'ស្ទឹងត្រែង', 'Stung Treng'),
    ('20', 'ស្វាយរៀង', 'Svay Rieng'),
    ('21', 'តាកែវ', 'Takeo'),
    ('22', 'ឧត្តរមានជ័យ', 'Oddar Meanchey'),
    ('23', 'កែប', 'Kep'),
    ('24', 'ប៉ៃលិន', 'Pailin'),
    ('25', 'ត្បូងឃ្មុំ', 'Tboung Khmum');

-- ----------------------------------------------------------------------------
-- Seed: Phnom Penh (province code 12) — all 14 khans (districts), and a
-- representative subset of sangkats (communes) for the districts most relevant
-- to the rental market. NOT exhaustive.
--
-- NOTE: the full Cambodian administrative hierarchy for the other 24 provinces
-- (~200 districts, ~1,650 communes total) and the remainder of Phnom Penh's
-- communes, plus all ~14,000 villages nationwide, must be imported from an
-- official NCDD/MoI dataset in a follow-up migration. Do not hand-type that
-- volume of data.
-- ----------------------------------------------------------------------------

INSERT INTO districts (province_id, code, name_km, name_en)
SELECT p.id, d.code, d.name_km, d.name_en
FROM provinces p
JOIN (VALUES
    ('1201', 'ដូនពេញ', 'Doun Penh'),
    ('1202', 'ប្រាំពីរមករា', 'Prampir Meakkakra'),
    ('1203', 'ចំការមន', 'Chamkar Mon'),
    ('1204', 'មានជ័យ', 'Mean Chey'),
    ('1205', 'ឫស្សីកែវ', 'Russey Keo'),
    ('1206', 'សែនសុខ', 'Sen Sok'),
    ('1207', 'ពោធិ៍សែនជ័យ', 'Pou Senchey'),
    ('1208', 'ដង្កោ', 'Dangkao'),
    ('1209', 'ជ្រោយចង្វារ', 'Chroy Changvar'),
    ('1210', 'ច្បារអំពៅ', 'Chbar Ampov'),
    ('1211', 'ព្រែកព្នៅ', 'Prek Pnov'),
    ('1212', 'ទួលគោក', 'Tuol Kork'),
    ('1213', 'បឹងកេងកង', 'Boeng Keng Kang'),
    ('1214', 'កំបូល', 'Kambol')
) AS d(code, name_km, name_en) ON true
WHERE p.code = '12';

INSERT INTO communes (district_id, code, name_km, name_en)
SELECT dist.id, c.code, c.name_km, c.name_en
FROM districts dist
JOIN (VALUES
    -- Doun Penh (1201)
    ('1201', '120101', 'ផ្សារធំបី', 'Phsar Thmei Bei'),
    ('1201', '120102', 'ផ្សារចាស់', 'Phsar Chas'),
    ('1201', '120103', 'ជ័យជំនះ', 'Chey Chumneah'),
    ('1201', '120104', 'ផ្សារកណ្ដាលទី១', 'Phsar Kandal Muoy'),
    -- Prampir Meakkakra / 7 Makara (1202)
    ('1202', '120201', 'វាលវង់', 'Veal Vong'),
    ('1202', '120202', 'មិត្តភាព', 'Mittapheap'),
    ('1202', '120203', 'បឹងព្រលិត', 'Boeng Prolit'),
    -- Chamkar Mon (1203)
    ('1203', '120301', 'ទន្លេបាសាក់', 'Tonle Basak'),
    ('1203', '120302', 'ទំនប់ទឹក', 'Tumnob Tuek'),
    ('1203', '120303', 'បឹងត្របែក', 'Boeng Trabaek'),
    ('1203', '120304', 'ផ្សារដើមថ្កូវ', 'Phsar Daeum Thkov'),
    -- Mean Chey (1204)
    ('1204', '120401', 'ស្ទឹងមានជ័យ', 'Stueng Mean Chey'),
    ('1204', '120402', 'បឹងទំពុន', 'Boeng Tumpun'),
    ('1204', '120403', 'ចាក់អង្រែលើ', 'Chak Angrae Leu'),
    ('1204', '120404', 'ចាក់អង្រែក្រោម', 'Chak Angrae Kraom'),
    -- Russey Keo (1205)
    ('1205', '120501', 'ទួលសង្កែ', 'Tuol Sangkae'),
    ('1205', '120502', 'ចង្រ្កានចំរេះទី១', 'Chrang Chamreh Muoy'),
    ('1205', '120503', 'ចង្រ្កានចំរេះទី២', 'Chrang Chamreh Pir'),
    -- Sen Sok (1206)
    ('1206', '120601', 'ភ្នំពេញថ្មី', 'Phnom Penh Thmei'),
    ('1206', '120602', 'ទឹកថ្លា', 'Tuek Thla'),
    ('1206', '120603', 'ក្រាំងធ្នង់', 'Krang Thnong'),
    -- Pou Senchey (1207)
    ('1207', '120701', 'កាកាបទី១', 'Kakab Muoy'),
    ('1207', '120702', 'កាកាបទី២', 'Kakab Pir'),
    ('1207', '120703', 'ត្រពាំងក្រសាំង', 'Trapaing Krasang'),
    -- Dangkao (1208)
    ('1208', '120801', 'ដង្កោ', 'Dangkao'),
    ('1208', '120802', 'ជាំចាវទី១', 'Choam Chao Muoy'),
    ('1208', '120803', 'ជាំចាវទី២', 'Choam Chao Pir'),
    -- Chroy Changvar (1209)
    ('1209', '120901', 'ជ្រោយចង្វារ', 'Chroy Changvar'),
    ('1209', '120902', 'ព្រែកតាសេក', 'Preaek Ta Sek'),
    -- Chbar Ampov (1210)
    ('1210', '121001', 'ច្បារអំពៅទី១', 'Chbar Ampov Muoy'),
    ('1210', '121002', 'និរោធ', 'Nirouth'),
    -- Prek Pnov (1211)
    ('1211', '121101', 'ព្រែកព្នៅ', 'Prek Pnov'),
    -- Tuol Kork (1212)
    ('1212', '121201', 'ទឹកល្អក់ទី១', 'Tuek Lak Muoy'),
    ('1212', '121202', 'ទឹកល្អក់ទី២', 'Tuek Lak Pir'),
    ('1212', '121203', 'បឹងកក់ទី១', 'Boeng Kak Muoy'),
    ('1212', '121204', 'បឹងកក់ទី២', 'Boeng Kak Pir'),
    ('1212', '121205', 'ផ្សារដេប៉ូទី១', 'Phsar Depou Muoy'),
    ('1212', '121206', 'ផ្សារដេប៉ូទី២', 'Phsar Depou Pir'),
    ('1212', '121207', 'ផ្សារដេប៉ូទី៣', 'Phsar Depou Bei'),
    -- Boeng Keng Kang (1213)
    ('1213', '121301', 'បឹងកេងកងទី១', 'Boeng Keng Kang Muoy'),
    ('1213', '121302', 'បឹងកេងកងទី២', 'Boeng Keng Kang Pir'),
    ('1213', '121303', 'បឹងកេងកងទី៣', 'Boeng Keng Kang Bei'),
    -- Kambol (1214)
    ('1214', '121401', 'កំបូល', 'Kambol'),
    ('1214', '121402', 'ត្រពាំងសាប', 'Trapaing Sab')
) AS c(dist_code, code, name_km, name_en) ON dist.code = c.dist_code;

-- A handful of illustrative villages under peri-urban Phnom Penh communes, to
-- exercise the commune -> village relationship. Numbered "Village N" naming is
-- a common real convention in these areas, but this list is a representative
-- placeholder for schema/testing purposes only — NOT verified against the
-- official NCDD gazetteer. Replace/extend from the official dataset before
-- relying on it for anything user-facing.
INSERT INTO villages (commune_id, code, name_km, name_en)
SELECT com.id, v.code, v.name_km, v.name_en
FROM communes com
JOIN (VALUES
    ('121401', '12140101', 'ភូមិទី១', 'Village 1'),
    ('121401', '12140102', 'ភូមិទី២', 'Village 2'),
    ('120802', '12080201', 'ភូមិទី១', 'Village 1'),
    ('120802', '12080202', 'ភូមិទី២', 'Village 2'),
    ('121101', '12110101', 'ភូមិទី១', 'Village 1'),
    ('121101', '12110102', 'ភូមិទី២', 'Village 2')
) AS v(com_code, code, name_km, name_en) ON com.code = v.com_code;

-- ----------------------------------------------------------------------------
-- Seed: property types
-- ----------------------------------------------------------------------------

INSERT INTO property_types (key, name_km, name_en, applies_to, icon) VALUES
    ('room',        'បន្ទប់ជួល',     'Room',       'RENT', 'bed'),
    ('apartment',   'អាផាតមិន',      'Apartment',  'BOTH', 'building-2'),
    ('flat_house',  'ផ្ទះល្វែង',      'Flat House', 'BOTH', 'home'),
    ('villa',       'វីឡា',          'Villa',      'BOTH', 'landmark'),
    ('shophouse',   'ផ្ទះលក់ដូរ',     'Shophouse',  'BOTH', 'store'),
    ('condo',       'ខុនដូ',          'Condo',      'BOTH', 'building'),
    ('office',      'ការិយាល័យ',      'Office',     'BOTH', 'briefcase'),
    ('land',        'ដីធ្លី',          'Land',       'SALE', 'map'),
    ('warehouse',   'ឃ្លាំង',         'Warehouse',  'BOTH', 'warehouse');

-- ----------------------------------------------------------------------------
-- Seed: amenities
-- ----------------------------------------------------------------------------

INSERT INTO amenities (key, name_km, name_en, icon, category) VALUES
    ('aircon',         'ម៉ាស៊ីនត្រជាក់',       'Aircon',         'snowflake',  'comfort'),
    ('wifi',            'វ៉ាយហ្វាយ',            'Wi-Fi',          'wifi',       'utility'),
    ('parking',         'កន្លែងចតឡាន',          'Parking',        'car',        'utility'),
    ('lift',             'ជណ្ដើរយន្ត',           'Lift',           'arrow-up-down', 'utility'),
    ('water_heater',   'ម៉ាស៊ីនកម្ដៅទឹក',      'Water Heater',   'droplet',    'comfort'),
    ('balcony',         'រានហាល',               'Balcony',        'door-open',  'comfort'),
    ('security_guard', 'អ្នកយាមសន្តិសុខ',     'Security Guard', 'shield',     'safety'),
    ('pet_allowed',    'អនុញ្ញាតសត្វចិញ្ចឹម', 'Pet Allowed',    'paw-print',  'policy'),
    ('gym',              'កន្លែងហាត់ប្រាណ',     'Gym',            'dumbbell',   'recreation'),
    ('pool',             'អាងហែលទឹក',            'Pool',           'waves',      'recreation');
