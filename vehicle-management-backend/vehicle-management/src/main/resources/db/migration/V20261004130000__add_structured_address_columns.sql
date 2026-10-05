-- Add structured Vietnam address columns to user_profiles and organizations
-- Supports both current (province -> ward) and legacy (province -> district -> ward) address formats
-- district_code is nullable: null = current address, not null = legacy address

-- user_profiles: add structured address fields
ALTER TABLE people.user_profiles
    ADD COLUMN IF NOT EXISTS address_detail text,
    ADD COLUMN IF NOT EXISTS ward_code varchar(20),
    ADD COLUMN IF NOT EXISTS district_code varchar(20),
    ADD COLUMN IF NOT EXISTS address_display text;

-- organizations: add structured address fields
ALTER TABLE iam.organizations
    ADD COLUMN IF NOT EXISTS address_detail text,
    ADD COLUMN IF NOT EXISTS ward_code varchar(20),
    ADD COLUMN IF NOT EXISTS district_code varchar(20),
    ADD COLUMN IF NOT EXISTS address_display text;

-- Indexes for user_profiles address fields
CREATE INDEX IF NOT EXISTS idx_user_profiles_ward_code ON people.user_profiles (ward_code);
CREATE INDEX IF NOT EXISTS idx_user_profiles_district_code ON people.user_profiles (district_code);

-- Indexes for organizations address fields
CREATE INDEX IF NOT EXISTS idx_organizations_ward_code ON iam.organizations (ward_code);
CREATE INDEX IF NOT EXISTS idx_organizations_district_code ON iam.organizations (district_code);

-- Check constraint: address fields must be all-null or all-not-null (except district_code which can be null for current addresses)
ALTER TABLE people.user_profiles
    ADD CONSTRAINT ck_user_profiles_address_structured
    CHECK (
        (address_detail IS NULL AND ward_code IS NULL AND district_code IS NULL AND address_display IS NULL)
        OR (address_detail IS NOT NULL AND ward_code IS NOT NULL AND address_display IS NOT NULL)
    );

ALTER TABLE iam.organizations
    ADD CONSTRAINT ck_organizations_address_structured
    CHECK (
        (address_detail IS NULL AND ward_code IS NULL AND district_code IS NULL AND address_display IS NULL)
        OR (address_detail IS NOT NULL AND ward_code IS NOT NULL AND address_display IS NOT NULL)
    );

-- Note: Foreign keys to reference tables are intentionally NOT created at database level
-- because a single ward_code column could reference either reference.wards (current)
-- or reference.legacy_wards (legacy), determined by district_code being null or not.
-- Validation is enforced at the application layer via VietnamAddressPolicy.

COMMENT ON COLUMN people.user_profiles.address_detail IS 'Street number and name (e.g., 123 Nguyen Van A)';
COMMENT ON COLUMN people.user_profiles.ward_code IS 'Ward/commune code (reference.wards for current, reference.legacy_wards for legacy)';
COMMENT ON COLUMN people.user_profiles.district_code IS 'District code for legacy addresses (reference.legacy_districts), NULL for current addresses';
COMMENT ON COLUMN people.user_profiles.address_display IS 'Full formatted display address built from reference data';

COMMENT ON COLUMN iam.organizations.address_detail IS 'Street number and name (e.g., 123 Nguyen Van A)';
COMMENT ON COLUMN iam.organizations.ward_code IS 'Ward/commune code (reference.wards for current, reference.legacy_wards for legacy)';
COMMENT ON COLUMN iam.organizations.district_code IS 'District code for legacy addresses (reference.legacy_districts), NULL for current addresses';
COMMENT ON COLUMN iam.organizations.address_display IS 'Full formatted display address built from reference data';
