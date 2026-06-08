-- V10: Support ticket categories and workflow audit

CREATE TABLE IF NOT EXISTS operations.support_ticket_categories (
                                                                    category_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                                    code VARCHAR(50) NOT NULL,
                                                                    name VARCHAR(150) NOT NULL,
                                                                    description TEXT,
                                                                    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                                                                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                    created_by UUID,
                                                                    updated_at TIMESTAMPTZ,
                                                                    updated_by UUID,

                                                                    CONSTRAINT ck_support_ticket_categories_priority
                                                                        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),

                                                                    CONSTRAINT ck_support_ticket_categories_status
                                                                        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

DO $$
DECLARE
    unique_constraint RECORD;
BEGIN
    FOR unique_constraint IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = 'operations'
          AND rel.relname = 'support_ticket_categories'
          AND con.contype = 'u'
          AND (
              SELECT array_agg(att.attname ORDER BY att.attname)
              FROM unnest(con.conkey) AS key(attnum)
              JOIN pg_attribute att
                ON att.attrelid = rel.oid
               AND att.attnum = key.attnum
          ) = ARRAY['code']::name[]
    LOOP
        EXECUTE format(
            'ALTER TABLE operations.support_ticket_categories DROP CONSTRAINT IF EXISTS %I',
            unique_constraint.conname
        );
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_support_ticket_categories_active_code
    ON operations.support_ticket_categories (code)
    WHERE status = 'ACTIVE';

INSERT INTO operations.support_ticket_categories (code, name, description, priority, status)
VALUES
    ('LOST_CARD', 'Mất thẻ xe', 'Khách hàng mất thẻ xe hoặc không xuất trình được thẻ khi ra bãi.', 'HIGH', 'ACTIVE'),
    ('WRONG_FEE', 'Khiếu nại phí gửi xe', 'Khách hàng phản ánh bị tính sai phí gửi xe.', 'HIGH', 'ACTIVE'),
    ('VEHICLE_DAMAGE', 'Xe hư hỏng/trầy xước', 'Khách hàng phản ánh xe bị trầy xước, hư hỏng hoặc mất tài sản.', 'URGENT', 'ACTIVE'),
    ('CARD_NOT_WORKING', 'Thẻ không hoạt động', 'Thẻ gửi xe không quét được hoặc không sử dụng được.', 'NORMAL', 'ACTIVE'),
    ('SUBSCRIPTION_PROBLEM', 'Vấn đề vé đăng ký', 'Vé tháng/quý/năm/miễn phí không hoạt động hoặc cần hỗ trợ.', 'NORMAL', 'ACTIVE'),
    ('PAYMENT_PROBLEM', 'Vấn đề thanh toán', 'Thanh toán lỗi, đã thanh toán nhưng chưa ghi nhận hoặc cần kiểm tra giao dịch.', 'HIGH', 'ACTIVE'),
    ('PARKING_HISTORY_REQUEST', 'Yêu cầu tra cứu lịch sử gửi xe', 'Khách hàng cần tra cứu lịch sử gửi xe.', 'LOW', 'ACTIVE'),
    ('PROFILE_OR_VEHICLE_UPDATE', 'Cập nhật hồ sơ/xe', 'Khách hàng cần hỗ trợ cập nhật thông tin cá nhân hoặc phương tiện.', 'LOW', 'ACTIVE'),
    ('STAFF_COMPLAINT', 'Khiếu nại nhân viên', 'Khách hàng phản ánh thái độ hoặc cách xử lý của nhân viên.', 'HIGH', 'ACTIVE'),
    ('OTHER', 'Vấn đề khác', 'Yêu cầu hỗ trợ khác chưa thuộc nhóm cố định.', 'NORMAL', 'ACTIVE')
ON CONFLICT (code) WHERE status = 'ACTIVE' DO NOTHING;

ALTER TABLE operations.support_tickets
    ADD COLUMN IF NOT EXISTS category_id UUID,
    ADD COLUMN IF NOT EXISTS resolution_note TEXT,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS closed_by UUID,
    ADD COLUMN IF NOT EXISTS reopen_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_reopened_at TIMESTAMPTZ;

UPDATE operations.support_tickets ticket
SET category_id = category.category_id
FROM operations.support_ticket_categories category
WHERE category.code = 'OTHER'
  AND ticket.category_id IS NULL;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_support_tickets_category'
        ) THEN
            ALTER TABLE operations.support_tickets
                ADD CONSTRAINT fk_support_tickets_category
                    FOREIGN KEY (category_id)
                        REFERENCES operations.support_ticket_categories(category_id)
                        ON DELETE RESTRICT;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_support_tickets_closed_by'
        ) THEN
            ALTER TABLE operations.support_tickets
                ADD CONSTRAINT fk_support_tickets_closed_by
                    FOREIGN KEY (closed_by)
                        REFERENCES iam.accounts(account_id)
                        ON DELETE SET NULL;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'ck_support_tickets_reopen_count_non_negative'
        ) THEN
            ALTER TABLE operations.support_tickets
                ADD CONSTRAINT ck_support_tickets_reopen_count_non_negative
                    CHECK (reopen_count >= 0);
        END IF;
    END $$;

ALTER TABLE operations.support_tickets
    ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE operations.support_tickets
    DROP CONSTRAINT IF EXISTS ck_support_tickets_priority;

ALTER TABLE operations.support_tickets
    DROP COLUMN IF EXISTS priority;

CREATE INDEX IF NOT EXISTS idx_support_ticket_categories_status
    ON operations.support_ticket_categories (status);

CREATE INDEX IF NOT EXISTS idx_support_ticket_categories_code
    ON operations.support_ticket_categories (code);

CREATE INDEX IF NOT EXISTS idx_support_tickets_category
    ON operations.support_tickets (category_id);

CREATE INDEX IF NOT EXISTS idx_support_tickets_status
    ON operations.support_tickets (status);

CREATE INDEX IF NOT EXISTS idx_support_tickets_customer
    ON operations.support_tickets (customer_id);

CREATE INDEX IF NOT EXISTS idx_support_tickets_assigned_to
    ON operations.support_tickets (assigned_to);
