ALTER TABLE parking.parking_lots
    ADD COLUMN address_input_scheme varchar(20),
    ADD COLUMN address_display varchar(500),
    ADD COLUMN current_ward_code varchar(20),
    ADD COLUMN legacy_ward_code varchar(20),
    ADD COLUMN geocoding_status varchar(30) NOT NULL DEFAULT 'NOT_REQUESTED',
    ADD COLUMN geocoded_at timestamptz;

UPDATE parking.parking_lots
SET address_display = NULLIF(btrim(address), ''),
    address_input_scheme = CASE WHEN NULLIF(btrim(address), '') IS NULL THEN NULL ELSE 'CURRENT' END,
    geocoding_status = CASE
        WHEN latitude IS NOT NULL AND longitude IS NOT NULL THEN 'NEEDS_REVIEW'
        ELSE 'NOT_REQUESTED'
    END;

-- Existing records have no trustworthy current ward association yet. They remain
-- editable drafts instead of being exposed as spatially valid active parking lots.
UPDATE parking.parking_lots
SET status = 'SETUP',
    activation_requested_at = NULL,
    activation_requested_by = NULL
WHERE status = 'ACTIVE'
  AND (address_display IS NULL OR current_ward_code IS NULL OR location IS NULL);

ALTER TABLE parking.parking_lots
    ADD CONSTRAINT fk_parking_lots_current_ward
        FOREIGN KEY (current_ward_code) REFERENCES reference.wards(code),
    ADD CONSTRAINT fk_parking_lots_legacy_ward
        FOREIGN KEY (legacy_ward_code) REFERENCES reference.legacy_wards(code),
    ADD CONSTRAINT ck_parking_lots_address_input_scheme
        CHECK (address_input_scheme IS NULL OR address_input_scheme IN ('CURRENT', 'LEGACY')),
    ADD CONSTRAINT ck_parking_lots_geocoding_status
        CHECK (geocoding_status IN ('NOT_REQUESTED', 'RESOLVED', 'MANUAL_CONFIRMED', 'FAILED', 'NEEDS_REVIEW')),
    ADD CONSTRAINT ck_parking_lots_address_codes
        CHECK (
            (address_input_scheme IS NULL AND current_ward_code IS NULL AND legacy_ward_code IS NULL)
            OR (address_input_scheme = 'CURRENT' AND legacy_ward_code IS NULL)
            OR (address_input_scheme = 'LEGACY' AND legacy_ward_code IS NOT NULL)
        ),
    ADD CONSTRAINT ck_parking_lots_active_address_ready
        CHECK (
            status <> 'ACTIVE'
            OR (
                NULLIF(btrim(address_display), '') IS NOT NULL
                AND latitude IS NOT NULL
                AND longitude IS NOT NULL
                AND location IS NOT NULL
                AND current_ward_code IS NOT NULL
                AND geocoding_status IN ('RESOLVED', 'MANUAL_CONFIRMED')
            )
        );

CREATE INDEX idx_parking_lots_current_ward_code ON parking.parking_lots(current_ward_code);
CREATE INDEX idx_parking_lots_legacy_ward_code ON parking.parking_lots(legacy_ward_code);

CREATE OR REPLACE FUNCTION parking.sync_parking_lot_address_display()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
BEGIN
    -- address remains a compatibility column for existing consumers. New code
    -- treats address_display as canonical and the database keeps both aligned.
    IF NEW.address_display IS DISTINCT FROM OLD.address_display THEN
        NEW.address := NEW.address_display;
    ELSIF NEW.address IS DISTINCT FROM OLD.address THEN
        NEW.address_display := NEW.address;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_parking_lots_sync_address_display
    BEFORE INSERT OR UPDATE OF address, address_display
    ON parking.parking_lots
    FOR EACH ROW
    EXECUTE FUNCTION parking.sync_parking_lot_address_display();
