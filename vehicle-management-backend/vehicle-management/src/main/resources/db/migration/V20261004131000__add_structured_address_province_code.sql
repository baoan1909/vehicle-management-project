ALTER TABLE people.user_profiles
    DROP CONSTRAINT ck_user_profiles_address_structured,
    ADD COLUMN province_code varchar(20);

ALTER TABLE iam.organizations
    DROP CONSTRAINT ck_organizations_address_structured,
    ADD COLUMN province_code varchar(20);

CREATE INDEX idx_user_profiles_province_code
    ON people.user_profiles (province_code);

CREATE INDEX idx_organizations_province_code
    ON iam.organizations (province_code);

ALTER TABLE people.user_profiles
    ADD CONSTRAINT ck_user_profiles_address_structured
    CHECK (
        (address_detail IS NULL AND province_code IS NULL AND ward_code IS NULL AND district_code IS NULL AND address_display IS NULL)
        OR (address_detail IS NOT NULL AND province_code IS NOT NULL AND ward_code IS NOT NULL AND address_display IS NOT NULL)
    );

ALTER TABLE iam.organizations
    ADD CONSTRAINT ck_organizations_address_structured
    CHECK (
        (address_detail IS NULL AND province_code IS NULL AND ward_code IS NULL AND district_code IS NULL AND address_display IS NULL)
        OR (address_detail IS NOT NULL AND province_code IS NOT NULL AND ward_code IS NOT NULL AND address_display IS NOT NULL)
    );

COMMENT ON COLUMN people.user_profiles.province_code
    IS 'Province/city code from the current or legacy reference hierarchy';

COMMENT ON COLUMN iam.organizations.province_code
    IS 'Province/city code from the current or legacy reference hierarchy';
