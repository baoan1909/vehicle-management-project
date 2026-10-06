-- Preserve legacy free-text addresses in the single display column before
-- removing the duplicated people.user_profiles.address column.
ALTER TABLE people.user_profiles
    DROP CONSTRAINT IF EXISTS ck_user_profiles_address_structured;

UPDATE people.user_profiles
SET address_display = NULLIF(btrim(address), '')
WHERE address_display IS NULL
  AND NULLIF(btrim(address), '') IS NOT NULL;

ALTER TABLE people.user_profiles
    DROP COLUMN address;

-- address_display may be display-only for migrated legacy rows. New and
-- updated addresses are validated by VietnamAddressPolicy and carry codes.
ALTER TABLE people.user_profiles
    ADD CONSTRAINT ck_user_profiles_address_structured
    CHECK (
        (
            address_display IS NULL
            AND address_detail IS NULL
            AND province_code IS NULL
            AND ward_code IS NULL
            AND district_code IS NULL
        )
        OR (
            address_display IS NOT NULL
            AND (
                (
                    address_detail IS NULL
                    AND province_code IS NULL
                    AND ward_code IS NULL
                    AND district_code IS NULL
                )
                OR (
                    address_detail IS NOT NULL
                    AND province_code IS NOT NULL
                    AND ward_code IS NOT NULL
                )
            )
        )
    );

COMMENT ON COLUMN people.user_profiles.address_display IS
    'Single display address; structured rows also carry province/ward and optional legacy district codes';
