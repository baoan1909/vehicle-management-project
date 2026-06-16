BEGIN;

ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS requested_effective_from DATE,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS rejected_by UUID,
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ;

UPDATE access_control.subscriptions
SET requested_effective_from = effective_from
WHERE requested_effective_from IS NULL;

ALTER TABLE access_control.subscriptions
    ALTER COLUMN requested_effective_from SET NOT NULL;

ALTER TABLE access_control.subscriptions
    DROP CONSTRAINT IF EXISTS ck_subscriptions_status;

ALTER TABLE access_control.subscriptions
    ADD CONSTRAINT ck_subscriptions_status
        CHECK (status IN (
                          'PENDING',
                          'PENDING_PAYMENT',
                          'PENDING_CARD',
                          'ACTIVE',
                          'EXPIRED',
                          'CANCELLED',
                          'REJECTED'
            ));

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_subscriptions_rejected_by'
        ) THEN
            ALTER TABLE access_control.subscriptions
                ADD CONSTRAINT fk_subscriptions_rejected_by
                    FOREIGN KEY (rejected_by)
                        REFERENCES iam.accounts(account_id)
                        ON DELETE SET NULL;
        END IF;
    END $$;

DO $$
    DECLARE
        constraint_name TEXT;
    BEGIN
        SELECT con.conname INTO constraint_name
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
                 JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = 'access_control'
          AND rel.relname = 'cards'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%AVAILABLE%'
        LIMIT 1;

        IF constraint_name IS NOT NULL THEN
            EXECUTE format(
                    'ALTER TABLE access_control.cards DROP CONSTRAINT %I',
                    constraint_name
                    );
        END IF;
    END $$;

ALTER TABLE access_control.cards
    ADD CONSTRAINT ck_cards_status
        CHECK (status IN (
                          'AVAILABLE',
                          'RESERVED',
                          'ASSIGNED',
                          'IN_USE',
                          'LOST',
                          'BLOCKED',
                          'DAMAGED',
                          'RETIRED'
            ));

CREATE INDEX IF NOT EXISTS idx_subscriptions_customer_vehicle_status_period
    ON access_control.subscriptions (
                                     customer_vehicle_id,
                                     status,
                                     effective_from,
                                     effective_to
        );

CREATE INDEX IF NOT EXISTS idx_subscriptions_card_status
    ON access_control.subscriptions (card_id, status);

CREATE INDEX IF NOT EXISTS idx_cards_vehicle_type_status
    ON access_control.cards (vehicle_type_id, status);

INSERT INTO iam.permission_actions(code, name, description)
VALUES
    ('APPROVE', 'Approve', 'Approve request.'),
    ('REJECT', 'Reject', 'Reject request.'),
    ('CANCEL', 'Cancel', 'Cancel request.'),
    ('ASSIGN', 'Assign', 'Assign card.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions(permission_code, module_id, action_id, scope_id, name, description)
SELECT v.permission_code, m.module_id, a.action_id, s.scope_id, v.name, v.description
FROM (
         VALUES
             ('SUBSCRIPTION_APPROVE_ALL', 'SUBSCRIPTION', 'APPROVE', 'ALL', 'Approve subscription', 'Approve subscription request.'),
             ('SUBSCRIPTION_REJECT_ALL', 'SUBSCRIPTION', 'REJECT', 'ALL', 'Reject subscription', 'Reject subscription request.'),
             ('SUBSCRIPTION_CANCEL_ALL', 'SUBSCRIPTION', 'CANCEL', 'ALL', 'Cancel subscription', 'Cancel any subscription before refund workflow.'),
             ('SUBSCRIPTION_CANCEL_OWN', 'SUBSCRIPTION', 'CANCEL', 'OWN', 'Cancel own subscription', 'Cancel own pending subscription.'),
             ('SUBSCRIPTION_ASSIGN_CARD_ALL', 'SUBSCRIPTION', 'ASSIGN', 'ALL', 'Assign subscription card', 'Assign reserved card to subscription.')
     ) AS v(permission_code, module_code, action_code, scope_code, name, description)
         JOIN iam.permission_modules m ON m.code = v.module_code
         JOIN iam.permission_actions a ON a.code = v.action_code
         JOIN iam.permission_scopes s ON s.code = v.scope_code
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions(id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
    'SUBSCRIPTION_CANCEL_OWN'
    )
WHERE r.code = 'CUSTOMER'
  AND NOT EXISTS (
    SELECT 1 FROM iam.role_permissions rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

INSERT INTO iam.role_permissions(id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'SUBSCRIPTION_CREATE_ALL',
                                                         'SUBSCRIPTION_READ_ALL',
                                                         'SUBSCRIPTION_ASSIGN_CARD_ALL'
    )
WHERE r.code = 'EMPLOYEE'
  AND NOT EXISTS (
    SELECT 1 FROM iam.role_permissions rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

INSERT INTO iam.role_permissions(id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'SUBSCRIPTION_CREATE_ALL',
                                                         'SUBSCRIPTION_READ_ALL',
                                                         'SUBSCRIPTION_UPDATE_ALL',
                                                         'SUBSCRIPTION_APPROVE_ALL',
                                                         'SUBSCRIPTION_REJECT_ALL',
                                                         'SUBSCRIPTION_CANCEL_ALL',
                                                         'SUBSCRIPTION_ASSIGN_CARD_ALL'
    )
WHERE r.code IN ('PARKING_MANAGER', 'SYSTEM_ADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM iam.role_permissions rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

COMMIT;