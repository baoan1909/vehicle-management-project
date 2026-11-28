-- Reset obsolete customer-owned development support data only on the first upgrade.
-- Reruns must preserve account-owned tickets and assistant conversations.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_attribute
        WHERE attrelid = 'operations.support_tickets'::regclass
          AND attname = 'customer_id'
          AND NOT attisdropped
    ) THEN
        DELETE FROM operations.approval_requests
        WHERE request_type = 'SUPPORT_TICKET_ESCALATION'
          AND target_schema = 'operations'
          AND target_table = 'support_tickets';

        DELETE FROM operations.support_ticket_conversation_links;

        UPDATE operations.chat_messages
        SET context_ticket_id = NULL
        WHERE context_ticket_id IS NOT NULL;

        DELETE FROM operations.support_tickets;

        -- Recreate personal support workspaces lazily with owner_account_id.
        DELETE FROM operations.chat_conversations
        WHERE conversation_type = 'ASSISTANT_SUPPORT';
    END IF;
END;
$$;

DROP INDEX IF EXISTS operations.uq_support_tickets_customer_idempotency;
DROP INDEX IF EXISTS operations.idx_support_tickets_customer_created_at;

ALTER TABLE operations.support_tickets
    DROP CONSTRAINT IF EXISTS fk_support_tickets_customer;

ALTER TABLE operations.support_tickets
    DROP COLUMN IF EXISTS customer_id,
    ADD COLUMN IF NOT EXISTS requester_account_id UUID NOT NULL,
    ADD COLUMN IF NOT EXISTS requester_role_code VARCHAR(50) NOT NULL,
    ADD COLUMN IF NOT EXISTS requester_name_snapshot VARCHAR(150),
    ADD COLUMN IF NOT EXISTS requester_email_snapshot VARCHAR(255),
    ADD COLUMN IF NOT EXISTS organization_id UUID,
    ADD COLUMN IF NOT EXISTS parking_lot_id UUID,
    ADD COLUMN IF NOT EXISTS feature_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS context_key VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.support_tickets'::regclass
          AND conname = 'fk_support_tickets_requester_account'
    ) THEN
        ALTER TABLE operations.support_tickets
            ADD CONSTRAINT fk_support_tickets_requester_account
                FOREIGN KEY (requester_account_id) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.support_tickets'::regclass
          AND conname = 'fk_support_tickets_organization'
    ) THEN
        ALTER TABLE operations.support_tickets
            ADD CONSTRAINT fk_support_tickets_organization
                FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.support_tickets'::regclass
          AND conname = 'fk_support_tickets_parking_lot'
    ) THEN
        ALTER TABLE operations.support_tickets
            ADD CONSTRAINT fk_support_tickets_parking_lot
                FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE SET NULL;
    END IF;
END;
$$;

ALTER TABLE operations.support_tickets
    DROP CONSTRAINT IF EXISTS ck_support_tickets_source;

ALTER TABLE operations.support_tickets
    ALTER COLUMN source SET DEFAULT 'SELF_SERVICE_PORTAL';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.support_tickets'::regclass
          AND conname = 'ck_support_tickets_source'
    ) THEN
        ALTER TABLE operations.support_tickets
            ADD CONSTRAINT ck_support_tickets_source CHECK (
                source IN ('SELF_SERVICE_PORTAL', 'ASSISTANT_CHAT', 'SUPPORT_CONVERSATION', 'SYSTEM_CREATED')
            );
    END IF;
END;
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_support_tickets_requester_idempotency
    ON operations.support_tickets (requester_account_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_support_tickets_requester_created_at
    ON operations.support_tickets (requester_account_id, created_at DESC, support_ticket_id DESC);
CREATE INDEX IF NOT EXISTS idx_support_tickets_requester_role
    ON operations.support_tickets (requester_role_code);
CREATE INDEX IF NOT EXISTS idx_support_tickets_status_created_at
    ON operations.support_tickets (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_tickets_organization
    ON operations.support_tickets (organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_support_tickets_parking_lot
    ON operations.support_tickets (parking_lot_id) WHERE parking_lot_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_support_tickets_feature
    ON operations.support_tickets (feature_code) WHERE feature_code IS NOT NULL;

ALTER TABLE operations.support_ticket_categories
    ADD COLUMN IF NOT EXISTS audience_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    ADD COLUMN IF NOT EXISTS feature_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS permission_match_mode VARCHAR(10) NOT NULL DEFAULT 'ANY',
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.support_ticket_categories'::regclass
          AND conname = 'ck_support_ticket_categories_audience'
    ) THEN
        ALTER TABLE operations.support_ticket_categories
            ADD CONSTRAINT ck_support_ticket_categories_audience CHECK (
                audience_type IN ('CUSTOMER', 'INTERNAL', 'ALL')
            );
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.support_ticket_categories'::regclass
          AND conname = 'ck_support_ticket_categories_permission_match'
    ) THEN
        ALTER TABLE operations.support_ticket_categories
            ADD CONSTRAINT ck_support_ticket_categories_permission_match CHECK (
                permission_match_mode IN ('ANY', 'ALL')
            );
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.support_ticket_categories'::regclass
          AND conname = 'ck_support_ticket_categories_display_order'
    ) THEN
        ALTER TABLE operations.support_ticket_categories
            ADD CONSTRAINT ck_support_ticket_categories_display_order CHECK (display_order >= 0);
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS operations.support_ticket_category_permissions (
    category_permission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES operations.support_ticket_categories(category_id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES iam.permissions(permission_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT uq_support_ticket_category_permission UNIQUE (category_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_support_ticket_category_permissions_permission
    ON operations.support_ticket_category_permissions (permission_id, category_id);

-- Audience defaults are permissions, never runtime role checks. Role codes below
-- only seed the initial permission matrix and can later be changed by administrators.
INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at
) VALUES
    ('d1710000-0000-4000-8000-000000000011', 'READ_CUSTOMER_SUPPORT_CATEGORY', 'Xem danh mục hỗ trợ khách hàng', 'Cho phép sử dụng nhóm danh mục hỗ trợ dành cho khách hàng.', now()),
    ('d1710000-0000-4000-8000-000000000012', 'READ_INTERNAL_SUPPORT_CATEGORY', 'Xem danh mục hỗ trợ vận hành', 'Cho phép sử dụng nhóm danh mục hỗ trợ dành cho người dùng vận hành.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description,
    module_id, action_id, scope_id, created_at
)
SELECT permission_seed.permission_id, permission_seed.permission_code,
       permission_seed.name, permission_seed.description,
       module.module_id, action.action_id, scope.scope_id, now()
FROM (VALUES
    ('d1710000-0000-4000-8000-000000000001'::uuid, 'SUPPORT_TICKET_CATEGORY_CUSTOMER_OWN', 'Dùng danh mục hỗ trợ khách hàng', 'Cho phép xem và chọn các danh mục hỗ trợ dành cho khách hàng.'),
    ('d1710000-0000-4000-8000-000000000002'::uuid, 'SUPPORT_TICKET_CATEGORY_INTERNAL_OWN', 'Dùng danh mục hỗ trợ nội bộ', 'Cho phép xem và chọn các danh mục hỗ trợ dành cho người dùng vận hành.')
) permission_seed(permission_id, permission_code, name, description)
JOIN iam.permission_modules module ON module.code = 'SUPPORT_TICKET'
JOIN iam.permission_actions action ON action.code = CASE permission_seed.permission_code
    WHEN 'SUPPORT_TICKET_CATEGORY_CUSTOMER_OWN' THEN 'READ_CUSTOMER_SUPPORT_CATEGORY'
    ELSE 'READ_INTERNAL_SUPPORT_CATEGORY'
END
JOIN iam.permission_scopes scope ON scope.code = 'OWN'
ON CONFLICT (permission_code) DO NOTHING;

-- Existing customer categories remain customer-facing.
UPDATE operations.support_ticket_categories
SET audience_type = 'CUSTOMER',
    display_order = CASE code
        WHEN 'LOST_CARD' THEN 10
        WHEN 'WRONG_FEE' THEN 20
        WHEN 'VEHICLE_DAMAGE' THEN 30
        WHEN 'CARD_NOT_WORKING' THEN 40
        WHEN 'SUBSCRIPTION_PROBLEM' THEN 50
        WHEN 'PAYMENT_PROBLEM' THEN 60
        WHEN 'PARKING_HISTORY_REQUEST' THEN 70
        WHEN 'PROFILE_OR_VEHICLE_UPDATE' THEN 80
        WHEN 'STAFF_COMPLAINT' THEN 90
        ELSE 100
    END,
    updated_at = now()
WHERE audience_type = 'ALL';

INSERT INTO operations.support_ticket_categories (
    code, name, description, priority, status, audience_type,
    feature_code, permission_match_mode, display_order
) VALUES
    ('PARKING_OPERATION', 'Vận hành xe vào/ra', 'Sự cố check-in, check-out, thẻ, biển số hoặc làn xe.', 'HIGH', 'ACTIVE', 'INTERNAL', 'PARKING_SESSION', 'ANY', 200),
    ('DEVICE_INCIDENT', 'Sự cố thiết bị', 'Camera, đầu đọc thẻ, barrier hoặc thiết bị vận hành gặp sự cố.', 'HIGH', 'ACTIVE', 'INTERNAL', 'DEVICE', 'ANY', 210),
    ('PRICING_CONFIGURATION', 'Cấu hình giá', 'Hỗ trợ kế hoạch giá, quy tắc giá và hiệu lực áp dụng.', 'NORMAL', 'ACTIVE', 'INTERNAL', 'PRICING', 'ANY', 220),
    ('TICKET_CONFIGURATION', 'Cấu hình loại vé', 'Hỗ trợ cấu hình và sử dụng loại vé.', 'NORMAL', 'ACTIVE', 'INTERNAL', 'TICKET_TYPE', 'ANY', 230),
    ('VEHICLE_CONFIGURATION', 'Cấu hình loại phương tiện', 'Hỗ trợ danh mục loại phương tiện.', 'NORMAL', 'ACTIVE', 'INTERNAL', 'VEHICLE_TYPE', 'ANY', 240),
    ('PARKING_LAYOUT', 'Bãi xe và sơ đồ', 'Hỗ trợ bãi xe, khu vực, vị trí và sơ đồ vận hành.', 'NORMAL', 'ACTIVE', 'INTERNAL', 'PARKING_LOT', 'ANY', 250),
    ('WORKFORCE', 'Nhân sự và ca trực', 'Hỗ trợ nhân sự, phân công và ca trực.', 'NORMAL', 'ACTIVE', 'INTERNAL', 'WORKFORCE', 'ANY', 260),
    ('ACCOUNT_ACCESS', 'Tài khoản và quyền truy cập', 'Hỗ trợ đăng nhập, phạm vi và quyền truy cập của tài khoản nội bộ.', 'HIGH', 'ACTIVE', 'INTERNAL', 'ACCOUNT_ACCESS', 'ANY', 270)
ON CONFLICT (code) WHERE status = 'ACTIVE' DO NOTHING;

INSERT INTO operations.support_ticket_category_permissions (category_id, permission_id)
SELECT category.category_id, permission.permission_id
FROM operations.support_ticket_categories category
JOIN iam.permissions permission ON permission.permission_code IN (
    'PARKING_SESSION_CHECK_IN_ALL', 'PARKING_SESSION_CHECK_OUT_ALL'
)
WHERE category.code = 'PARKING_OPERATION' AND category.status = 'ACTIVE'
ON CONFLICT (category_id, permission_id) DO NOTHING;

INSERT INTO operations.support_ticket_category_permissions (category_id, permission_id)
SELECT category.category_id, permission.permission_id
FROM operations.support_ticket_categories category
JOIN iam.permissions permission ON permission.permission_code = 'DEVICE_READ_ALL'
WHERE category.code = 'DEVICE_INCIDENT' AND category.status = 'ACTIVE'
ON CONFLICT (category_id, permission_id) DO NOTHING;

INSERT INTO operations.support_ticket_category_permissions (category_id, permission_id)
SELECT category.category_id, permission.permission_id
FROM operations.support_ticket_categories category
JOIN iam.permissions permission ON permission.permission_code IN ('PRICE_PLAN_READ_ALL', 'PRICE_RULE_READ_ALL')
WHERE category.code = 'PRICING_CONFIGURATION' AND category.status = 'ACTIVE'
ON CONFLICT (category_id, permission_id) DO NOTHING;

INSERT INTO operations.support_ticket_category_permissions (category_id, permission_id)
SELECT category.category_id, permission.permission_id
FROM operations.support_ticket_categories category
JOIN iam.permissions permission ON permission.permission_code = 'TICKET_TYPE_READ_ALL'
WHERE category.code = 'TICKET_CONFIGURATION' AND category.status = 'ACTIVE'
ON CONFLICT (category_id, permission_id) DO NOTHING;

INSERT INTO operations.support_ticket_category_permissions (category_id, permission_id)
SELECT category.category_id, permission.permission_id
FROM operations.support_ticket_categories category
JOIN iam.permissions permission ON permission.permission_code = 'VEHICLE_TYPE_READ_ALL'
WHERE category.code = 'VEHICLE_CONFIGURATION' AND category.status = 'ACTIVE'
ON CONFLICT (category_id, permission_id) DO NOTHING;

INSERT INTO operations.support_ticket_category_permissions (category_id, permission_id)
SELECT category.category_id, permission.permission_id
FROM operations.support_ticket_categories category
JOIN iam.permissions permission ON permission.permission_code IN ('PARKING_LOT_READ_ALL', 'PARKING_SPACE_READ_ALL')
WHERE category.code = 'PARKING_LAYOUT' AND category.status = 'ACTIVE'
ON CONFLICT (category_id, permission_id) DO NOTHING;

INSERT INTO operations.support_ticket_category_permissions (category_id, permission_id)
SELECT category.category_id, permission.permission_id
FROM operations.support_ticket_categories category
JOIN iam.permissions permission ON permission.permission_code IN ('EMPLOYEE_READ_ALL', 'SHIFT_READ_ALL')
WHERE category.code = 'WORKFORCE' AND category.status = 'ACTIVE'
ON CONFLICT (category_id, permission_id) DO NOTHING;

DROP INDEX IF EXISTS operations.uq_chat_conversations_active_assistant_customer;
DROP INDEX IF EXISTS operations.uq_chat_conversations_assistant_customer;

ALTER TABLE operations.chat_conversations
    DROP CONSTRAINT IF EXISTS ck_chat_conversations_type;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.chat_conversations'::regclass
          AND conname = 'ck_chat_conversations_type'
    ) THEN
        ALTER TABLE operations.chat_conversations
            ADD CONSTRAINT ck_chat_conversations_type CHECK (
                conversation_type IN (
                    'INTERNAL_DIRECT', 'INTERNAL_GROUP', 'CUSTOMER_DIRECT', 'ASSISTANT_SUPPORT',
                    'SUPPORT_REQUESTER_DIRECT', 'SUPPORT_TICKET', 'PARKING_SESSION', 'BILLING',
                    'LOST_CARD', 'SYSTEM_DIRECT'
                )
            );
    END IF;
END;
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_conversations_active_assistant_owner
    ON operations.chat_conversations (owner_account_id)
    WHERE conversation_type = 'ASSISTANT_SUPPORT' AND status = 'ACTIVE';

-- Grant self-service support to every requester role.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), NULL, NULL, NULL, TRUE, TRUE
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'SUPPORT_WIDGET_ACCESS_OWN', 'SUPPORT_TICKET_CREATE_OWN',
    'SUPPORT_TICKET_CREATE_FROM_CHAT_OWN', 'SUPPORT_TICKET_READ_OWN',
    'SUPPORT_TICKET_UPDATE_OWN', 'SUPPORT_TICKET_CLOSE_OWN', 'SUPPORT_TICKET_REOPEN_OWN',
    'CHAT_CONVERSATION_READ_OWN', 'CHAT_MESSAGE_SEND_OWN',
    'CHAT_ATTACHMENT_CREATE_OWN', 'CHAT_ATTACHMENT_READ_OWN'
)
WHERE role.code IN ('CUSTOMER', 'PARTNER_ADMIN', 'PARKING_MANAGER', 'EMPLOYEE')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE, updated_at = now();

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), NULL, NULL, NULL, TRUE, TRUE
FROM iam.roles role
JOIN iam.permissions permission
  ON permission.permission_code = CASE
      WHEN role.code = 'CUSTOMER' THEN 'SUPPORT_TICKET_CATEGORY_CUSTOMER_OWN'
      ELSE 'SUPPORT_TICKET_CATEGORY_INTERNAL_OWN'
  END
WHERE role.code IN ('CUSTOMER', 'PARTNER_ADMIN', 'PARKING_MANAGER', 'EMPLOYEE')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE, updated_at = now();

-- Only system administrators handle the global support queue.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), NULL, NULL, NULL, TRUE, TRUE
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'SUPPORT_TICKET_READ_ALL', 'SUPPORT_TICKET_ASSIGN', 'SUPPORT_TICKET_CLAIM_OWN',
    'SUPPORT_TICKET_PROCESS_ALL', 'SUPPORT_TICKET_UPDATE_ALL',
    'SUPPORT_TICKET_RESPOND_ASSIGNED', 'SUPPORT_TICKET_CLOSE_ALL',
    'SUPPORT_TICKET_REOPEN_ALL', 'SUPPORT_TICKET_ESCALATION_REVIEW_ALL',
    'CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT'
)
WHERE role.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE, updated_at = now();

UPDATE iam.role_permissions role_permission
SET is_active = FALSE, updated_at = now()
FROM iam.roles role, iam.permissions permission
WHERE role_permission.role_id = role.role_id
  AND role_permission.permission_id = permission.permission_id
  AND role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER', 'EMPLOYEE')
  AND permission.permission_code IN (
      'SUPPORT_TICKET_READ_ASSIGNED', 'SUPPORT_TICKET_PROCESS_ASSIGNED',
      'SUPPORT_TICKET_RESPOND_ASSIGNED', 'SUPPORT_TICKET_CLAIM_OWN',
      'SUPPORT_TICKET_READ_ALL', 'SUPPORT_TICKET_ASSIGN', 'SUPPORT_TICKET_PROCESS_ALL',
      'SUPPORT_TICKET_UPDATE_ALL', 'SUPPORT_TICKET_CLOSE_ALL', 'SUPPORT_TICKET_REOPEN_ALL',
      'SUPPORT_TICKET_ESCALATION_REVIEW_ALL'
  );

UPDATE iam.permissions
SET name = CASE permission_code
        WHEN 'SUPPORT_TICKET_CREATE_OWN' THEN 'Tạo phiếu hỗ trợ của mình'
        WHEN 'SUPPORT_TICKET_READ_OWN' THEN 'Xem phiếu hỗ trợ của mình'
        WHEN 'SUPPORT_TICKET_UPDATE_OWN' THEN 'Cập nhật phiếu hỗ trợ của mình'
        WHEN 'SUPPORT_TICKET_CLOSE_OWN' THEN 'Đóng phiếu hỗ trợ của mình'
        WHEN 'SUPPORT_TICKET_REOPEN_OWN' THEN 'Mở lại phiếu hỗ trợ của mình'
        ELSE name
    END,
    description = replace(description, 'khách hàng', 'người yêu cầu'),
    updated_at = now()
WHERE permission_code IN (
    'SUPPORT_TICKET_CREATE_OWN', 'SUPPORT_TICKET_READ_OWN', 'SUPPORT_TICKET_UPDATE_OWN',
    'SUPPORT_TICKET_CLOSE_OWN', 'SUPPORT_TICKET_REOPEN_OWN',
    'SUPPORT_TICKET_CREATE_FROM_CHAT_OWN', 'SUPPORT_TICKET_ESCALATION_CREATE_OWN'
);
