CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE iam.parking_lot_management_assignments (
    assignment_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    organization_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    manager_account_id uuid NOT NULL,
    management_role varchar(20) NOT NULL,
    status varchar(30) NOT NULL,
    effective_from timestamptz NOT NULL,
    effective_to timestamptz,
    accepted_at timestamptz,
    assigned_by uuid NOT NULL,
    ended_by uuid,
    end_reason varchar(500),
    handover_note varchar(1000),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT ck_management_assignment_role
        CHECK (management_role IN ('PRIMARY', 'SUPPORT')),
    CONSTRAINT ck_management_assignment_status
        CHECK (status IN ('PENDING_ACCEPTANCE', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT ck_management_assignment_period
        CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT ck_management_assignment_ended
        CHECK ((status <> 'ENDED') OR (effective_to IS NOT NULL AND ended_by IS NOT NULL)),
    CONSTRAINT fk_management_assignment_organization
        FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id) ON DELETE RESTRICT,
    CONSTRAINT fk_management_assignment_lot_tenant
        FOREIGN KEY (organization_id, parking_lot_id)
        REFERENCES parking.parking_lots(organization_id, parking_lot_id) ON DELETE RESTRICT,
    CONSTRAINT fk_management_assignment_manager
        FOREIGN KEY (manager_account_id) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_management_assignment_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_management_assignment_ended_by
        FOREIGN KEY (ended_by) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT ex_primary_management_assignment_period
        EXCLUDE USING gist (
            parking_lot_id WITH =,
            tstzrange(effective_from, COALESCE(effective_to, 'infinity'::timestamptz), '[)') WITH &&
        ) WHERE (management_role = 'PRIMARY' AND status IN ('PENDING_ACCEPTANCE', 'ACTIVE')),
    CONSTRAINT ex_manager_lot_assignment_period
        EXCLUDE USING gist (
            manager_account_id WITH =,
            parking_lot_id WITH =,
            tstzrange(effective_from, COALESCE(effective_to, 'infinity'::timestamptz), '[)') WITH &&
        ) WHERE (status IN ('PENDING_ACCEPTANCE', 'ACTIVE'))
);

CREATE INDEX idx_management_assignment_manager_effective
    ON iam.parking_lot_management_assignments(manager_account_id, status, effective_from, effective_to);
CREATE INDEX idx_management_assignment_lot_effective
    ON iam.parking_lot_management_assignments(parking_lot_id, status, effective_from, effective_to);
CREATE INDEX idx_management_assignment_org_lot
    ON iam.parking_lot_management_assignments(organization_id, parking_lot_id);

CREATE TABLE people.employee_parking_lot_assignments (
    assignment_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    organization_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    assignment_type varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    effective_from timestamptz NOT NULL,
    effective_to timestamptz,
    assigned_by_account_id uuid NOT NULL,
    assignment_source varchar(20) NOT NULL,
    ended_by_account_id uuid,
    end_reason varchar(500),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT ck_employee_lot_assignment_type
        CHECK (assignment_type IN ('PRIMARY', 'SUPPORT', 'TEMPORARY')),
    CONSTRAINT ck_employee_lot_assignment_status
        CHECK (status IN ('SCHEDULED', 'ACTIVE', 'SUSPENDED', 'ENDED', 'CANCELLED', 'PENDING_REVIEW')),
    CONSTRAINT ck_employee_lot_assignment_source
        CHECK (assignment_source IN ('PARTNER', 'MANAGER', 'MIGRATION')),
    CONSTRAINT ck_employee_lot_assignment_period
        CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT ck_employee_lot_assignment_ended
        CHECK ((status <> 'ENDED') OR effective_to IS NOT NULL),
    CONSTRAINT fk_employee_lot_assignment_organization
        FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_lot_assignment_lot_tenant
        FOREIGN KEY (organization_id, parking_lot_id)
        REFERENCES parking.parking_lots(organization_id, parking_lot_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_lot_assignment_employee
        FOREIGN KEY (employee_id) REFERENCES people.employees(employee_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_lot_assignment_assigned_by
        FOREIGN KEY (assigned_by_account_id) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_lot_assignment_ended_by
        FOREIGN KEY (ended_by_account_id) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT ex_employee_lot_assignment_period
        EXCLUDE USING gist (
            employee_id WITH =,
            parking_lot_id WITH =,
            tstzrange(effective_from, COALESCE(effective_to, 'infinity'::timestamptz), '[)') WITH &&
        ) WHERE (status IN ('SCHEDULED', 'ACTIVE'))
);

CREATE INDEX idx_employee_lot_assignment_employee_effective
    ON people.employee_parking_lot_assignments(employee_id, status, effective_from, effective_to);
CREATE INDEX idx_employee_lot_assignment_lot_effective
    ON people.employee_parking_lot_assignments(parking_lot_id, status, effective_from, effective_to);
CREATE INDEX idx_employee_lot_assignment_org_lot
    ON people.employee_parking_lot_assignments(organization_id, parking_lot_id);

ALTER TABLE parking.parking_lots
    ADD COLUMN management_state varchar(30) NOT NULL DEFAULT 'PARTNER_SUPERVISED',
    ADD COLUMN management_revision bigint NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_parking_lot_management_state
        CHECK (management_state IN ('MANAGED', 'HANDOVER', 'PARTNER_SUPERVISED', 'OPERATION_SUSPENDED'));

CREATE TABLE operations.parking_lot_work_queue (
    work_item_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    organization_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    queue_type varchar(50) NOT NULL,
    current_assignee uuid,
    status varchar(20) NOT NULL DEFAULT 'OPEN',
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    handled_by uuid,
    handled_at timestamptz,
    reassigned_at timestamptz,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT ck_parking_lot_work_queue_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED')),
    CONSTRAINT fk_work_queue_organization
        FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id) ON DELETE RESTRICT,
    CONSTRAINT fk_work_queue_lot_tenant
        FOREIGN KEY (organization_id, parking_lot_id)
        REFERENCES parking.parking_lots(organization_id, parking_lot_id) ON DELETE RESTRICT,
    CONSTRAINT fk_work_queue_assignee
        FOREIGN KEY (current_assignee) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_work_queue_handled_by
        FOREIGN KEY (handled_by) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT
);

CREATE INDEX idx_work_queue_lot_status
    ON operations.parking_lot_work_queue(parking_lot_id, status, created_at);
CREATE INDEX idx_work_queue_assignee_status
    ON operations.parking_lot_work_queue(current_assignee, status);

ALTER TABLE operations.shift_assignments
    ADD COLUMN employee_parking_lot_assignment_id uuid,
    ADD CONSTRAINT fk_shift_assignment_employee_lot_assignment
        FOREIGN KEY (employee_parking_lot_assignment_id)
        REFERENCES people.employee_parking_lot_assignments(assignment_id) ON DELETE RESTRICT;
CREATE INDEX idx_shift_assignment_employee_lot_assignment
    ON operations.shift_assignments(employee_parking_lot_assignment_id);

CREATE OR REPLACE FUNCTION iam.validate_management_assignment_tenant_role()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM iam.organization_memberships membership
        JOIN iam.accounts account ON account.account_id = membership.account_id
        JOIN iam.roles role ON role.role_id = account.role_id
        WHERE membership.organization_id = NEW.organization_id
          AND membership.account_id = NEW.manager_account_id
          AND membership.status = 'ACTIVE'
          AND account.status = 'ACTIVE'
          AND role.code = 'PARKING_MANAGER'
          AND role.is_active = true
    ) THEN
        RAISE EXCEPTION 'MANAGER_NOT_ACTIVE_IN_ORGANIZATION' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_management_assignment_tenant_role
BEFORE INSERT OR UPDATE OF organization_id, manager_account_id, status
ON iam.parking_lot_management_assignments
FOR EACH ROW
WHEN (NEW.status IN ('PENDING_ACCEPTANCE', 'ACTIVE'))
EXECUTE FUNCTION iam.validate_management_assignment_tenant_role();

CREATE OR REPLACE FUNCTION people.validate_employee_lot_assignment_tenant()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM people.employees employee
        JOIN iam.accounts account ON account.user_profile_id = employee.user_profile_id
        JOIN iam.organization_memberships membership ON membership.account_id = account.account_id
        JOIN iam.roles role ON role.role_id = account.role_id
        WHERE employee.employee_id = NEW.employee_id
          AND membership.organization_id = NEW.organization_id
          AND membership.status = 'ACTIVE'
          AND role.code = 'EMPLOYEE'
    ) THEN
        RAISE EXCEPTION 'EMPLOYEE_NOT_IN_ORGANIZATION' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_employee_lot_assignment_tenant
BEFORE INSERT OR UPDATE OF organization_id, employee_id, status
ON people.employee_parking_lot_assignments
FOR EACH ROW
WHEN (NEW.status IN ('SCHEDULED', 'ACTIVE'))
EXECUTE FUNCTION people.validate_employee_lot_assignment_tenant();

-- Backfill Manager history from the old explicit membership-lot scope. The old
-- scope is used only as evidence for Manager assignments, never for Employees.
INSERT INTO iam.parking_lot_management_assignments (
    assignment_id, organization_id, parking_lot_id, manager_account_id,
    management_role, status, effective_from, accepted_at, assigned_by,
    created_at, created_by
)
SELECT gen_random_uuid(), membership.organization_id, scope.parking_lot_id, membership.account_id,
       'SUPPORT', 'ACTIVE', COALESCE(scope.created_at, now()), COALESCE(scope.created_at, now()),
       COALESCE(scope.created_by, membership.created_by, membership.account_id),
       COALESCE(scope.created_at, now()), scope.created_by
FROM iam.member_parking_lot_scopes scope
JOIN iam.organization_memberships membership
  ON membership.organization_membership_id = scope.organization_membership_id
JOIN iam.accounts account ON account.account_id = membership.account_id
JOIN iam.roles role ON role.role_id = account.role_id
WHERE role.code = 'PARKING_MANAGER'
  AND membership.status = 'ACTIVE'
  AND account.status = 'ACTIVE'
ON CONFLICT DO NOTHING;

-- Fail-closed Employee backfill. Only active/future shift evidence creates an
-- operable assignment. Historical-only evidence remains ENDED for audit.
WITH evidenced AS (
    SELECT assignment.employee_id,
           shift.parking_lot_id,
           lot.organization_id,
           MIN(shift.start_time) AS effective_from,
           MAX(shift.end_time) AS effective_to,
           BOOL_OR(shift.status IN ('OPEN', 'SCHEDULED') AND shift.end_time > now()) AS active_or_future,
           (array_agg(
               COALESCE(assignment.created_by, shift.created_by)
               ORDER BY shift.start_time
           ) FILTER (WHERE COALESCE(assignment.created_by, shift.created_by) IS NOT NULL))[1] AS assigned_by
    FROM operations.shift_assignments assignment
    JOIN operations.shifts shift ON shift.shift_id = assignment.shift_id
    JOIN parking.parking_lots lot ON lot.parking_lot_id = shift.parking_lot_id
    WHERE assignment.status <> 'REMOVED'
    GROUP BY assignment.employee_id, shift.parking_lot_id, lot.organization_id
)
INSERT INTO people.employee_parking_lot_assignments (
    assignment_id, organization_id, parking_lot_id, employee_id,
    assignment_type, status, effective_from, effective_to,
    assigned_by_account_id, assignment_source, created_at, created_by
)
SELECT gen_random_uuid(), evidenced.organization_id, evidenced.parking_lot_id, evidenced.employee_id,
       'SUPPORT', CASE WHEN evidenced.active_or_future THEN 'ACTIVE' ELSE 'ENDED' END,
       evidenced.effective_from,
       CASE WHEN evidenced.active_or_future THEN NULL ELSE evidenced.effective_to END,
       COALESCE(evidenced.assigned_by, account.account_id), 'MIGRATION', now(), evidenced.assigned_by
FROM evidenced
JOIN people.employees employee ON employee.employee_id = evidenced.employee_id
JOIN iam.accounts account ON account.user_profile_id = employee.user_profile_id
JOIN iam.organization_memberships membership
  ON membership.account_id = account.account_id
 AND membership.organization_id = evidenced.organization_id
WHERE membership.status = 'ACTIVE'
ON CONFLICT DO NOTHING;

UPDATE operations.shift_assignments assignment
SET employee_parking_lot_assignment_id = employee_lot.assignment_id
FROM operations.shifts shift,
     people.employee_parking_lot_assignments employee_lot
WHERE assignment.shift_id = shift.shift_id
  AND employee_lot.employee_id = assignment.employee_id
  AND employee_lot.parking_lot_id = shift.parking_lot_id
  AND employee_lot.effective_from <= shift.start_time
  AND (employee_lot.effective_to IS NULL OR employee_lot.effective_to >= shift.end_time)
  AND assignment.employee_parking_lot_assignment_id IS NULL;

UPDATE parking.parking_lots lot
SET management_state = CASE
        WHEN EXISTS (
            SELECT 1 FROM iam.parking_lot_management_assignments assignment
            WHERE assignment.parking_lot_id = lot.parking_lot_id
              AND assignment.status = 'ACTIVE'
              AND assignment.effective_from <= now()
              AND (assignment.effective_to IS NULL OR assignment.effective_to > now())
        ) THEN 'MANAGED'
        ELSE 'PARTNER_SUPERVISED'
    END,
    management_revision = management_revision + 1;

-- Operations/remediation views intentionally expose only IDs and invariant
-- violations. They grant no scope and are safe for Partner review tooling.
CREATE VIEW operations.employee_lot_assignment_remediation AS
SELECT assignment.shift_assignment_id, assignment.employee_id, shift.parking_lot_id,
       shift.start_time, shift.end_time, shift.status AS shift_status
FROM operations.shift_assignments assignment
JOIN operations.shifts shift ON shift.shift_id = assignment.shift_id
WHERE assignment.status <> 'REMOVED'
  AND assignment.employee_parking_lot_assignment_id IS NULL;

CREATE VIEW iam.parking_scope_tenant_violations AS
SELECT 'MANAGER'::varchar AS assignment_kind, assignment.assignment_id,
       assignment.organization_id, assignment.parking_lot_id
FROM iam.parking_lot_management_assignments assignment
JOIN parking.parking_lots lot ON lot.parking_lot_id = assignment.parking_lot_id
WHERE assignment.organization_id <> lot.organization_id
UNION ALL
SELECT 'EMPLOYEE', assignment.assignment_id, assignment.organization_id, assignment.parking_lot_id
FROM people.employee_parking_lot_assignments assignment
JOIN parking.parking_lots lot ON lot.parking_lot_id = assignment.parking_lot_id
WHERE assignment.organization_id <> lot.organization_id;

-- Employees receive action permissions only; database scope resolvers still
-- constrain every read/write to explicit lot assignments and active shifts.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'PARKING_SCOPE_ASSIGNED',
    'PARKING_LOT_READ_ALL',
    'PARKING_SESSION_READ_ALL',
    'PARKING_SESSION_CREATE_ALL',
    'PARKING_SESSION_UPDATE_ALL',
    'PARKING_SESSION_CHECK_IN_ALL',
    'PARKING_SESSION_CHECK_OUT_ALL',
    'LOST_CARD_REPORT_READ_ALL',
    'LOST_CARD_REPORT_CREATE_ALL',
    'CARD_READ_ALL',
    'PRICE_PLAN_READ_ALL',
    'PRICE_RULE_READ_ALL',
    'VEHICLE_TYPE_READ_ALL',
    'TICKET_TYPE_READ_ALL',
    'CARD_TYPE_READ_ALL',
    'SHIFT_ASSIGNMENT_READ_OWN'
)
WHERE role.code = 'EMPLOYEE'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true, updated_at = now();
