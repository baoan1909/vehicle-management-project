-- Keep one canonical display address per business object. Structured rows also
-- retain their address detail and administrative division codes.

ALTER TABLE iam.organizations
    DROP CONSTRAINT IF EXISTS ck_organizations_address_structured;

UPDATE iam.organizations
SET address_display = NULLIF(btrim(address), '')
WHERE address_display IS NULL
  AND NULLIF(btrim(address), '') IS NOT NULL;

ALTER TABLE iam.organizations
    DROP COLUMN address;

ALTER TABLE iam.organizations
    ADD CONSTRAINT ck_organizations_address_structured
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

COMMENT ON COLUMN iam.organizations.address_display IS
    'Single display address; structured rows also carry province/ward and optional legacy district codes';

UPDATE parking.parking_lots
SET address_display = NULLIF(btrim(address), '')
WHERE address_display IS NULL
  AND NULLIF(btrim(address), '') IS NOT NULL;

DROP TRIGGER IF EXISTS trg_parking_lots_sync_address_display
    ON parking.parking_lots;

DROP FUNCTION IF EXISTS parking.sync_parking_lot_address_display();

ALTER TABLE parking.parking_lots
    DROP COLUMN address;

COMMENT ON COLUMN parking.parking_lots.address_display IS
    'Canonical formatted parking-lot address shown by admin and public APIs';
