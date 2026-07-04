BEGIN;

ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS requested_effective_from DATE,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS rejected_by UUID,
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ;

-- Giữ lại ngày đăng ký cũ cho dữ liệu đã tồn tại.
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

CREATE INDEX IF NOT EXISTS idx_subscriptions_customer_status
    ON access_control.subscriptions(customer_id, status);

CREATE INDEX IF NOT EXISTS idx_subscriptions_vehicle_status
    ON access_control.subscriptions(customer_vehicle_id, status);

CREATE INDEX IF NOT EXISTS idx_subscriptions_effective_period
    ON access_control.subscriptions(effective_from, effective_to);

CREATE INDEX IF NOT EXISTS idx_subscriptions_requested_effective
    ON access_control.subscriptions(requested_effective_from, status);

-- Các action đã có thì câu lệnh sẽ bỏ qua.
INSERT INTO iam.permission_actions(code, name, description)
VALUES
    ('APPROVE', 'Phê duyệt', 'Cho phép phê duyệt yêu cầu.'),
    ('REJECT', 'Từ chối', 'Cho phép từ chối yêu cầu.'),
    ('CANCEL', 'Hủy', 'Cho phép hủy yêu cầu chưa thanh toán.'),
    ('ASSIGN', 'Gán', 'Cho phép gán thẻ.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions(
    permission_code, module_id, action_id, scope_id, name, description
)
SELECT v.permission_code, m.module_id, a.action_id, s.scope_id,
       v.name, v.description
FROM (
         VALUES
             ('SUBSCRIPTION_APPROVE_ALL','SUBSCRIPTION','APPROVE','ALL','Duyệt đăng ký vé','Duyệt yêu cầu đăng ký vé.'),
             ('SUBSCRIPTION_REJECT_ALL','SUBSCRIPTION','REJECT','ALL','Từ chối đăng ký vé','Từ chối yêu cầu đăng ký vé.'),
             ('SUBSCRIPTION_CANCEL_ALL','SUBSCRIPTION','CANCEL','ALL','Hủy đăng ký vé','Hủy đăng ký chưa thanh toán.'),
             ('SUBSCRIPTION_CANCEL_OWN','SUBSCRIPTION','CANCEL','OWN','Hủy đăng ký của mình','Khách hủy đăng ký chưa thanh toán.'),
             ('SUBSCRIPTION_ASSIGN_CARD_ALL','SUBSCRIPTION','ASSIGN','ALL','Gán thẻ đăng ký','Gán thẻ cho đăng ký đã thanh toán.')
     ) v(permission_code, module_code, action_code, scope_code, name, description)
         JOIN iam.permission_modules m ON m.code = v.module_code
         JOIN iam.permission_actions a ON a.code = v.action_code
         JOIN iam.permission_scopes s ON s.code = v.scope_code
ON CONFLICT (permission_code) DO NOTHING;

COMMIT;