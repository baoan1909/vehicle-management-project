-- Vehicle Management owns license-plate identity and display rules. OCR remains unchanged.
-- Legacy columns are retained for API/database compatibility during the deprecation window.

ALTER TABLE people.customer_vehicles
    ALTER COLUMN license_plate DROP NOT NULL,
    ADD COLUMN license_plate_normalized varchar(20)
        GENERATED ALWAYS AS (NULLIF(regexp_replace(upper(license_plate), '[ .-]', '', 'g'), '')) STORED;

ALTER TABLE parking.parking_sessions
    ALTER COLUMN license_plate_in DROP NOT NULL,
    ADD COLUMN license_plate_in_normalized varchar(20)
        GENERATED ALWAYS AS (NULLIF(regexp_replace(upper(license_plate_in), '[ .-]', '', 'g'), '')) STORED,
    ADD COLUMN license_plate_out_normalized varchar(20)
        GENERATED ALWAYS AS (NULLIF(regexp_replace(upper(license_plate_out), '[ .-]', '', 'g'), '')) STORED;

ALTER TABLE parking.parking_events
    ADD COLUMN license_plate_detected_normalized varchar(20)
        GENERATED ALWAYS AS (NULLIF(regexp_replace(upper(license_plate_detected), '[ .-]', '', 'g'), '')) STORED;

ALTER TABLE billing.invoices
    ADD COLUMN license_plate_normalized_snapshot varchar(20),
    ADD COLUMN license_plate_display_snapshot varchar(30),
    ADD COLUMN license_plate_format_snapshot varchar(40),
    ADD COLUMN license_plate_format_version varchar(20);

ALTER TABLE people.customer_vehicles
    ADD CONSTRAINT ck_customer_vehicle_plate_normalized_ascii
        CHECK (license_plate_normalized IS NULL OR license_plate_normalized ~ '^[A-Z0-9]+$');
ALTER TABLE parking.parking_sessions
    ADD CONSTRAINT ck_parking_session_plate_in_normalized_ascii
        CHECK (license_plate_in_normalized IS NULL OR license_plate_in_normalized ~ '^[A-Z0-9]+$'),
    ADD CONSTRAINT ck_parking_session_plate_out_normalized_ascii
        CHECK (license_plate_out_normalized IS NULL OR license_plate_out_normalized ~ '^[A-Z0-9]+$');
ALTER TABLE parking.parking_events
    ADD CONSTRAINT ck_parking_event_plate_normalized_ascii
        CHECK (license_plate_detected_normalized IS NULL OR license_plate_detected_normalized ~ '^[A-Z0-9]+$');

DO $$
DECLARE
    duplicate_plates text;
BEGIN
    SELECT string_agg(license_plate_normalized, ', ' ORDER BY license_plate_normalized)
    INTO duplicate_plates
    FROM (
        SELECT license_plate_normalized
        FROM people.customer_vehicles
        WHERE license_plate_normalized IS NOT NULL
        GROUP BY license_plate_normalized
        HAVING count(*) > 1
        LIMIT 20
    ) collisions;

    IF duplicate_plates IS NOT NULL THEN
        RAISE EXCEPTION 'Normalized customer vehicle plate collisions must be resolved before migration: %', duplicate_plates;
    END IF;
END
$$;

ALTER TABLE people.customer_vehicles
    DROP CONSTRAINT IF EXISTS customer_vehicles_license_plate_key;

CREATE UNIQUE INDEX ux_customer_vehicles_license_plate_normalized
    ON people.customer_vehicles (license_plate_normalized)
    WHERE license_plate_normalized IS NOT NULL;

CREATE INDEX ix_parking_sessions_plate_in_normalized_status
    ON parking.parking_sessions (license_plate_in_normalized, status);
CREATE INDEX ix_parking_sessions_plate_out_normalized
    ON parking.parking_sessions (license_plate_out_normalized)
    WHERE license_plate_out_normalized IS NOT NULL;
CREATE INDEX ix_parking_events_plate_detected_normalized
    ON parking.parking_events (license_plate_detected_normalized)
    WHERE license_plate_detected_normalized IS NOT NULL;

-- Unknown-but-safe plates can be reviewed by operators. Identity-changing overrides are manager-only.
INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES
    ('6bcbb302-0000-4000-8000-000000000001', 'REVIEW_PLATE', 'Xác nhận biển số', 'Xác nhận biển số an toàn nhưng chưa nhận diện được định dạng.', now(), NULL, NULL, NULL),
    ('6bcbb302-0000-4000-8000-000000000002', 'OVERRIDE_PLATE', 'Ghi đè biển số', 'Ghi đè biển số khi có sai lệch định danh.', now(), NULL, NULL, NULL);

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description,
    created_at, created_by, updated_at, updated_by, module_id, action_id, scope_id
) VALUES
    ('6bcbb303-0000-4000-8000-000000000001', 'PARKING_SESSION_PLATE_REVIEW_ALL',
     'Xác nhận biển số chưa rõ định dạng', 'Cho phép nhân viên vận hành xác nhận biển số an toàn cần review.',
     now(), NULL, NULL, NULL, '00000000-0000-0000-0000-000000001022',
     '6bcbb302-0000-4000-8000-000000000001', '00000000-0000-0000-0000-000000003001'),
    ('6bcbb303-0000-4000-8000-000000000002', 'PARKING_SESSION_PLATE_OVERRIDE_ALL',
     'Ghi đè định danh biển số', 'Cho phép quản lý ghi đè biển số khi OCR và hồ sơ không khớp.',
     now(), NULL, NULL, NULL, '00000000-0000-0000-0000-000000001022',
     '6bcbb302-0000-4000-8000-000000000002', '00000000-0000-0000-0000-000000003001');

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code = 'PARKING_SESSION_PLATE_REVIEW_ALL'
WHERE role.code IN ('EMPLOYEE', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true;

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code = 'PARKING_SESSION_PLATE_OVERRIDE_ALL'
WHERE role.code = 'PARKING_MANAGER'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true;
