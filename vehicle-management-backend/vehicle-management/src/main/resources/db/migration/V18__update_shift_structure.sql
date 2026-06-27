CREATE TABLE operations.shift_templates (
                                            shift_template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            parking_lot_id UUID NOT NULL,
                                            shift_type VARCHAR(20) NOT NULL,
                                            name VARCHAR(100) NOT NULL,
                                            start_local_time TIME NOT NULL,
                                            end_local_time TIME NOT NULL,
                                            status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            created_by UUID,
                                            updated_at TIMESTAMPTZ,
                                            updated_by UUID,

                                            CONSTRAINT fk_shift_templates_parking_lot
                                                FOREIGN KEY (parking_lot_id)
                                                    REFERENCES parking.parking_lots(parking_lot_id)
                                                    ON DELETE RESTRICT,

                                            CONSTRAINT fk_shift_templates_created_by
                                                FOREIGN KEY (created_by)
                                                    REFERENCES iam.accounts(account_id)
                                                    ON DELETE SET NULL,

                                            CONSTRAINT fk_shift_templates_updated_by
                                                FOREIGN KEY (updated_by)
                                                    REFERENCES iam.accounts(account_id)
                                                    ON DELETE SET NULL,

                                            CONSTRAINT ck_shift_templates_shift_type
                                                CHECK (shift_type IN ('MORNING', 'AFTERNOON', 'NIGHT')),

                                            CONSTRAINT ck_shift_templates_status
                                                CHECK (status IN ('ACTIVE', 'INACTIVE')),

                                            CONSTRAINT ck_shift_templates_name_not_blank
                                                CHECK (btrim(name) <> ''),

                                            CONSTRAINT ck_shift_templates_time_not_equal
                                                CHECK (start_local_time <> end_local_time),

                                            CONSTRAINT ck_shift_templates_duration
                                                CHECK (
                                                    CASE
                                                        WHEN end_local_time > start_local_time
                                                            THEN end_local_time - start_local_time
                                                        ELSE end_local_time - start_local_time + INTERVAL '24 hours'
                                                        END = INTERVAL '8 hours'
                                                    )
);

CREATE UNIQUE INDEX uq_shift_templates_active_lot_type
    ON operations.shift_templates (parking_lot_id, shift_type)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_shift_templates_lot_status
    ON operations.shift_templates (parking_lot_id, status);

CREATE INDEX idx_shift_templates_type_status
    ON operations.shift_templates (shift_type, status);

BEGIN;

-- =========================================================
-- 1. Employee roster rules
-- =========================================================

CREATE TABLE operations.employee_roster_rules (
                                                  roster_rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  parking_lot_id UUID NOT NULL,
                                                  employee_id UUID NOT NULL,
                                                  preferred_shift_type VARCHAR(20),
                                                  preferred_gate_id UUID,
                                                  weekly_day_off VARCHAR(10) NOT NULL,
                                                  assignment_mode VARCHAR(20) NOT NULL,
                                                  effective_from DATE NOT NULL,
                                                  effective_to DATE,
                                                  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                  created_by UUID,
                                                  updated_at TIMESTAMPTZ,
                                                  updated_by UUID,

                                                  CONSTRAINT fk_roster_rules_parking_lot
                                                      FOREIGN KEY (parking_lot_id)
                                                          REFERENCES parking.parking_lots(parking_lot_id)
                                                          ON DELETE RESTRICT,

                                                  CONSTRAINT fk_roster_rules_employee
                                                      FOREIGN KEY (employee_id)
                                                          REFERENCES people.employees(employee_id)
                                                          ON DELETE RESTRICT,

                                                  CONSTRAINT fk_roster_rules_gate
                                                      FOREIGN KEY (preferred_gate_id)
                                                          REFERENCES parking.gates(gate_id)
                                                          ON DELETE RESTRICT,

                                                  CONSTRAINT fk_roster_rules_created_by
                                                      FOREIGN KEY (created_by)
                                                          REFERENCES iam.accounts(account_id)
                                                          ON DELETE SET NULL,

                                                  CONSTRAINT fk_roster_rules_updated_by
                                                      FOREIGN KEY (updated_by)
                                                          REFERENCES iam.accounts(account_id)
                                                          ON DELETE SET NULL,

                                                  CONSTRAINT ck_roster_rules_shift_type
                                                      CHECK (
                                                          preferred_shift_type IS NULL
                                                              OR preferred_shift_type IN ('MORNING', 'AFTERNOON', 'NIGHT')
                                                          ),

                                                  CONSTRAINT ck_roster_rules_weekly_day_off
                                                      CHECK (
                                                          weekly_day_off IN (
                                                                             'MONDAY',
                                                                             'TUESDAY',
                                                                             'WEDNESDAY',
                                                                             'THURSDAY',
                                                                             'FRIDAY',
                                                                             'SATURDAY',
                                                                             'SUNDAY'
                                                              )
                                                          ),

                                                  CONSTRAINT ck_roster_rules_assignment_mode
                                                      CHECK (assignment_mode IN ('FIXED', 'RELIEF')),

                                                  CONSTRAINT ck_roster_rules_status
                                                      CHECK (status IN ('ACTIVE', 'INACTIVE')),

                                                  CONSTRAINT ck_roster_rules_effective_period
                                                      CHECK (
                                                          effective_to IS NULL
                                                              OR effective_to >= effective_from
                                                          ),

                                                  CONSTRAINT ck_roster_rules_mode_fields
                                                      CHECK (
                                                          (
                                                              assignment_mode = 'FIXED'
                                                                  AND preferred_shift_type IS NOT NULL
                                                                  AND preferred_gate_id IS NOT NULL
                                                              )
                                                              OR
                                                          (
                                                              assignment_mode = 'RELIEF'
                                                                  AND preferred_shift_type IS NULL
                                                                  AND preferred_gate_id IS NULL
                                                              )
                                                          )
);

CREATE INDEX idx_roster_rules_lot_status
    ON operations.employee_roster_rules (
                                         parking_lot_id,
                                         status
        );

CREATE INDEX idx_roster_rules_employee_period
    ON operations.employee_roster_rules (
                                         employee_id,
                                         effective_from,
                                         effective_to
        );

CREATE INDEX idx_roster_rules_fixed_position
    ON operations.employee_roster_rules (
                                         parking_lot_id,
                                         preferred_shift_type,
                                         preferred_gate_id,
                                         status
        );

CREATE INDEX idx_roster_rules_effective_period
    ON operations.employee_roster_rules (
                                         effective_from,
                                         effective_to
        );

-- Các ràng buộc chồng khoảng hiệu lực được kiểm tra ở application policy,
-- vì chúng phụ thuộc đồng thời employee, mode, vị trí và khoảng ngày.

-- =========================================================
-- 2. Update operations.shifts
-- =========================================================

ALTER TABLE operations.shifts
    ADD COLUMN shift_template_id UUID,
    ADD COLUMN shift_date DATE,
    ADD COLUMN shift_type VARCHAR(20),
    ADD COLUMN approved_at TIMESTAMPTZ,
    ADD COLUMN approved_by UUID,
    ADD COLUMN opened_at TIMESTAMPTZ,
    ADD COLUMN opened_by UUID,
    ADD COLUMN closed_at TIMESTAMPTZ,
    ADD COLUMN closed_by UUID,
    ADD COLUMN cancelled_at TIMESTAMPTZ,
    ADD COLUMN cancelled_by UUID,
    ADD COLUMN cancellation_reason TEXT,
    ADD COLUMN note TEXT;

-- Backfill dữ liệu shift cũ.
UPDATE operations.shifts
SET shift_date =
        (start_time AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE,
    shift_type =
        CASE
            WHEN EXTRACT(
                         HOUR FROM start_time AT TIME ZONE 'Asia/Ho_Chi_Minh'
                 ) >= 6
                AND EXTRACT(
                            HOUR FROM start_time AT TIME ZONE 'Asia/Ho_Chi_Minh'
                    ) < 14
                THEN 'MORNING'

            WHEN EXTRACT(
                         HOUR FROM start_time AT TIME ZONE 'Asia/Ho_Chi_Minh'
                 ) >= 14
                AND EXTRACT(
                            HOUR FROM start_time AT TIME ZONE 'Asia/Ho_Chi_Minh'
                    ) < 22
                THEN 'AFTERNOON'

            ELSE 'NIGHT'
            END,
    end_time = COALESCE(
            end_time,
            start_time + INTERVAL '8 hours'
               );

-- Backfill dữ liệu vòng đời cho shift cũ.
UPDATE operations.shifts
SET approved_at = COALESCE(created_at, start_time),
    approved_by = created_by
WHERE status IN ('OPEN', 'CLOSED');

UPDATE operations.shifts
SET opened_at = start_time,
    opened_by = created_by
WHERE status IN ('OPEN', 'CLOSED');

UPDATE operations.shifts
SET closed_at = COALESCE(updated_at, end_time),
    closed_by = COALESCE(updated_by, created_by)
WHERE status = 'CLOSED';

UPDATE operations.shifts
SET cancelled_at = COALESCE(updated_at, created_at),
    cancelled_by = COALESCE(updated_by, created_by),
    cancellation_reason = 'Migrated legacy cancelled shift',
    opening_cash = NULL
WHERE status = 'CANCELLED';

ALTER TABLE operations.shifts
    ALTER COLUMN shift_date SET NOT NULL,
    ALTER COLUMN shift_type SET NOT NULL,
    ALTER COLUMN end_time SET NOT NULL,
    ALTER COLUMN opening_cash DROP NOT NULL,
    ALTER COLUMN opening_cash DROP DEFAULT;

ALTER TABLE operations.shifts
    DROP CONSTRAINT IF EXISTS ck_shifts_status;

ALTER TABLE operations.shifts
    ADD CONSTRAINT ck_shifts_status
        CHECK (
            status IN (
                       'DRAFT',
                       'SCHEDULED',
                       'OPEN',
                       'CLOSED',
                       'CANCELLED'
                )
            ),

    ADD CONSTRAINT ck_shifts_shift_type
        CHECK (
            shift_type IN (
                           'MORNING',
                           'AFTERNOON',
                           'NIGHT'
                )
            ),

    ADD CONSTRAINT ck_shifts_time_order
        CHECK (end_time > start_time),

    ADD CONSTRAINT ck_shifts_opening_cash
        CHECK (
            opening_cash IS NULL
                OR opening_cash >= 0
            ),

    ADD CONSTRAINT ck_shifts_closing_cash
        CHECK (
            closing_cash IS NULL
                OR closing_cash >= 0
            ),

    ADD CONSTRAINT fk_shifts_template
        FOREIGN KEY (shift_template_id)
            REFERENCES operations.shift_templates(shift_template_id)
            ON DELETE RESTRICT,

    ADD CONSTRAINT fk_shifts_approved_by
        FOREIGN KEY (approved_by)
            REFERENCES iam.accounts(account_id)
            ON DELETE SET NULL,

    ADD CONSTRAINT fk_shifts_opened_by
        FOREIGN KEY (opened_by)
            REFERENCES iam.accounts(account_id)
            ON DELETE SET NULL,

    ADD CONSTRAINT fk_shifts_closed_by
        FOREIGN KEY (closed_by)
            REFERENCES iam.accounts(account_id)
            ON DELETE SET NULL,

    ADD CONSTRAINT fk_shifts_cancelled_by
        FOREIGN KEY (cancelled_by)
            REFERENCES iam.accounts(account_id)
            ON DELETE SET NULL,

    ADD CONSTRAINT fk_shifts_updated_by
        FOREIGN KEY (updated_by)
            REFERENCES iam.accounts(account_id)
            ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_shifts_lot_date_type
    ON operations.shifts (
                          parking_lot_id,
                          shift_date,
                          shift_type
        );

CREATE INDEX idx_shifts_lot_date_status
    ON operations.shifts (
                          parking_lot_id,
                          shift_date,
                          status
        );

CREATE INDEX idx_shifts_template
    ON operations.shifts (shift_template_id);

CREATE INDEX idx_shifts_time_range
    ON operations.shifts (start_time, end_time);

-- shift_template_id được để nullable nhằm giữ các shift cũ.
-- Shift mới được sinh bởi backend bắt buộc phải có shift_template_id.

-- =========================================================
-- 3. Update operations.shift_assignments
-- =========================================================

ALTER TABLE operations.shift_assignments
    ADD COLUMN gate_id UUID,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN updated_by UUID;

-- Chuyển audit cũ sang cấu trúc AuditableEntity.
UPDATE operations.shift_assignments
SET created_at = COALESCE(assigned_at, now());

UPDATE operations.shift_assignments assignment_item
SET created_by = shift_item.created_by
FROM operations.shifts shift_item
WHERE assignment_item.shift_id = shift_item.shift_id;

ALTER TABLE operations.shift_assignments
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE operations.shift_assignments
    DROP CONSTRAINT IF EXISTS uq_shift_assignments;

ALTER TABLE operations.shift_assignments
    DROP COLUMN role_in_shift,
    DROP COLUMN assigned_at;

ALTER TABLE operations.shift_assignments
    ADD CONSTRAINT ck_shift_assignments_status
        CHECK (status IN ('ACTIVE', 'REMOVED')),

    ADD CONSTRAINT fk_shift_assignments_gate
        FOREIGN KEY (gate_id)
            REFERENCES parking.gates(gate_id)
            ON DELETE RESTRICT,

    ADD CONSTRAINT fk_shift_assignments_created_by
        FOREIGN KEY (created_by)
            REFERENCES iam.accounts(account_id)
            ON DELETE SET NULL,

    ADD CONSTRAINT fk_shift_assignments_updated_by
        FOREIGN KEY (updated_by)
            REFERENCES iam.accounts(account_id)
            ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_shift_assignments_active_employee
    ON operations.shift_assignments (
                                     shift_id,
                                     employee_id
        )
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_shift_assignments_active_gate
    ON operations.shift_assignments (
                                     shift_id,
                                     gate_id
        )
    WHERE status = 'ACTIVE';

CREATE INDEX idx_shift_assignments_employee_status
    ON operations.shift_assignments (
                                     employee_id,
                                     status
        );

CREATE INDEX idx_shift_assignments_gate_status
    ON operations.shift_assignments (
                                     gate_id,
                                     status
        );

COMMIT;