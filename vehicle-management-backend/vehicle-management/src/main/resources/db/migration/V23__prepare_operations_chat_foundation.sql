BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO iam.permission_modules (code, name, description)
VALUES
    ('CHAT_CONVERSATION', 'Chat conversations', 'Manage operational chat conversations.'),
    ('CHAT_MESSAGE', 'Chat messages', 'Send, read, delete, and moderate chat messages.'),
    ('CHAT_ATTACHMENT', 'Chat attachments', 'Upload and access files attached to chat messages.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions (code, name, description)
VALUES
    ('SEND', 'Send', 'Allows sending chat messages.'),
    ('ASSIGN', 'Assign', 'Allows assigning a chat conversation to a handler.'),
    ('MODERATE', 'Moderate', 'Allows moderating chat messages or attachments.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_code,
    module_id,
    action_id,
    scope_id,
    name,
    description
)
SELECT
    permission_value.permission_code,
    module_item.module_id,
    action_item.action_id,
    scope_item.scope_id,
    permission_value.name,
    permission_value.description
FROM (
         VALUES
             ('CHAT_CONVERSATION_CREATE_OWN', 'CHAT_CONVERSATION', 'CREATE', 'OWN', 'Create own chat conversation', 'Allows creating a chat conversation for the current account.'),
             ('CHAT_CONVERSATION_CREATE_ALL', 'CHAT_CONVERSATION', 'CREATE', 'ALL', 'Create chat conversation', 'Allows creating chat conversations for supported business flows.'),
             ('CHAT_CONVERSATION_READ_OWN', 'CHAT_CONVERSATION', 'READ', 'OWN', 'Read own chat conversation', 'Allows reading conversations where the current account is a participant.'),
             ('CHAT_CONVERSATION_READ_ALL', 'CHAT_CONVERSATION', 'READ', 'ALL', 'Read all chat conversations', 'Allows reading chat conversations for management and support operations.'),
             ('CHAT_CONVERSATION_UPDATE_ALL', 'CHAT_CONVERSATION', 'UPDATE', 'ALL', 'Update chat conversation', 'Allows updating chat conversation metadata, status, or members.'),
             ('CHAT_CONVERSATION_ASSIGN_ALL', 'CHAT_CONVERSATION', 'ASSIGN', 'ALL', 'Assign chat conversation', 'Allows assigning chat conversations to employees or managers.'),

             ('CHAT_MESSAGE_SEND_OWN', 'CHAT_MESSAGE', 'SEND', 'OWN', 'Send chat message', 'Allows sending messages in conversations where the current account is an active participant.'),
             ('CHAT_MESSAGE_DELETE_OWN', 'CHAT_MESSAGE', 'DELETE', 'OWN', 'Delete own chat message', 'Allows soft-deleting messages sent by the current account.'),
             ('CHAT_MESSAGE_MODERATE_ALL', 'CHAT_MESSAGE', 'MODERATE', 'ALL', 'Moderate chat messages', 'Allows moderating messages across operational chat conversations.'),

             ('CHAT_ATTACHMENT_CREATE_OWN', 'CHAT_ATTACHMENT', 'CREATE', 'OWN', 'Upload chat attachment', 'Allows uploading attachments in conversations where the current account is an active participant.'),
             ('CHAT_ATTACHMENT_READ_OWN', 'CHAT_ATTACHMENT', 'READ', 'OWN', 'Read chat attachment', 'Allows reading attachments in conversations where the current account is an active participant.'),
             ('CHAT_ATTACHMENT_DELETE_OWN', 'CHAT_ATTACHMENT', 'DELETE', 'OWN', 'Delete own chat attachment', 'Allows removing attachments from messages sent by the current account.'),
             ('CHAT_ATTACHMENT_MODERATE_ALL', 'CHAT_ATTACHMENT', 'MODERATE', 'ALL', 'Moderate chat attachments', 'Allows moderating attachments across operational chat conversations.')
     ) AS permission_value(
                           permission_code,
                           module_code,
                           action_code,
                           scope_code,
                           name,
                           description
    )
         JOIN iam.permission_modules module_item
              ON module_item.code = permission_value.module_code
         JOIN iam.permission_actions action_item
              ON action_item.code = permission_value.action_code
         JOIN iam.permission_scopes scope_item
              ON scope_item.code = permission_value.scope_code
ON CONFLICT (permission_code) DO NOTHING;

CREATE TABLE IF NOT EXISTS operations.chat_conversations (
    conversation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_type VARCHAR(30) NOT NULL,
    title VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    customer_id UUID,
    support_ticket_id UUID,
    owner_account_id UUID,
    assigned_to UUID,
    related_schema VARCHAR(50),
    related_table VARCHAR(80),
    related_id UUID,
    last_message_id UUID,
    last_message_at TIMESTAMPTZ,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_chat_conversations_customer
        FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conversations_support_ticket
        FOREIGN KEY (support_ticket_id) REFERENCES operations.support_tickets(support_ticket_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conversations_owner_account
        FOREIGN KEY (owner_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_conversations_assigned_to
        FOREIGN KEY (assigned_to) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_chat_conversations_type
        CHECK (conversation_type IN ('INTERNAL_DIRECT', 'INTERNAL_GROUP', 'CUSTOMER_DIRECT', 'SUPPORT_TICKET', 'PARKING_SESSION', 'BILLING', 'LOST_CARD', 'SYSTEM_DIRECT')),
    CONSTRAINT ck_chat_conversations_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS operations.chat_conversation_members (
    conversation_member_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    account_id UUID NOT NULL,
    member_role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_read_message_id UUID,
    muted_until TIMESTAMPTZ,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_chat_members_conversation
        FOREIGN KEY (conversation_id) REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_members_account
        FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT uq_chat_members_conversation_account
        UNIQUE (conversation_id, account_id),
    CONSTRAINT ck_chat_members_role
        CHECK (member_role IN ('OWNER', 'MEMBER', 'ASSIGNEE', 'OBSERVER', 'CUSTOMER')),
    CONSTRAINT ck_chat_members_status
        CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED', 'BLOCKED'))
);

CREATE TABLE IF NOT EXISTS operations.chat_messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    sender_account_id UUID,
    message_type VARCHAR(30) NOT NULL,
    content TEXT,
    reply_to_message_id UUID,
    related_schema VARCHAR(50),
    related_table VARCHAR(80),
    related_id UUID,
    metadata JSONB,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    edited_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_chat_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender
        FOREIGN KEY (sender_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_messages_reply_to
        FOREIGN KEY (reply_to_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL,
    CONSTRAINT ck_chat_messages_type
        CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM', 'CONTEXT_CARD', 'ACTION_CARD', 'SUPPORT_REQUEST'))
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_chat_conversations_last_message') THEN
        ALTER TABLE operations.chat_conversations
            ADD CONSTRAINT fk_chat_conversations_last_message
                FOREIGN KEY (last_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_chat_members_last_read_message') THEN
        ALTER TABLE operations.chat_conversation_members
            ADD CONSTRAINT fk_chat_members_last_read_message
                FOREIGN KEY (last_read_message_id) REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS operations.chat_message_attachments (
    attachment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL,
    bucket VARCHAR(20) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    checksum_sha256 VARCHAR(64),
    attachment_type VARCHAR(30) NOT NULL DEFAULT 'IMAGE',
    width INT,
    height INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_chat_attachments_message
        FOREIGN KEY (message_id) REFERENCES operations.chat_messages(message_id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_attachments_bucket
        CHECK (bucket IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_chat_attachments_type
        CHECK (attachment_type IN ('IMAGE', 'DOCUMENT', 'AUDIO', 'PARKING_EVIDENCE', 'PAYMENT_PROOF')),
    CONSTRAINT ck_chat_attachments_size_non_negative
        CHECK (size_bytes IS NULL OR size_bytes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_chat_conversations_last_message_at
    ON operations.chat_conversations(last_message_at DESC, conversation_id DESC);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_customer
    ON operations.chat_conversations(customer_id);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_support_ticket
    ON operations.chat_conversations(support_ticket_id);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_related
    ON operations.chat_conversations(related_schema, related_table, related_id);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_assigned_to_status
    ON operations.chat_conversations(assigned_to, status);
CREATE INDEX IF NOT EXISTS idx_chat_members_account_status
    ON operations.chat_conversation_members(account_id, status);
CREATE INDEX IF NOT EXISTS idx_chat_members_conversation_status
    ON operations.chat_conversation_members(conversation_id, status);
CREATE INDEX IF NOT EXISTS idx_chat_members_conversation_account_status
    ON operations.chat_conversation_members(conversation_id, account_id, status);
CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_created_at
    ON operations.chat_messages(conversation_id, created_at DESC, message_id DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender_created_at
    ON operations.chat_messages(sender_account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_related
    ON operations.chat_messages(related_schema, related_table, related_id);
CREATE INDEX IF NOT EXISTS idx_chat_attachments_message
    ON operations.chat_message_attachments(message_id);
CREATE INDEX IF NOT EXISTS idx_chat_attachments_object_key
    ON operations.chat_message_attachments(object_key);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_chat_conversations_set_updated_at') THEN
        CREATE TRIGGER trg_chat_conversations_set_updated_at
            BEFORE UPDATE ON operations.chat_conversations
            FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_chat_conversation_members_set_updated_at') THEN
        CREATE TRIGGER trg_chat_conversation_members_set_updated_at
            BEFORE UPDATE ON operations.chat_conversation_members
            FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_chat_messages_set_updated_at') THEN
        CREATE TRIGGER trg_chat_messages_set_updated_at
            BEFORE UPDATE ON operations.chat_messages
            FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_chat_message_attachments_set_updated_at') THEN
        CREATE TRIGGER trg_chat_message_attachments_set_updated_at
            BEFORE UPDATE ON operations.chat_message_attachments
            FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
    END IF;
END $$;

INSERT INTO iam.role_permissions (id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), role_item.role_id, permission_item.permission_id, TRUE, TRUE
FROM iam.roles role_item
         JOIN iam.permissions permission_item
              ON permission_item.permission_code IN (
                                                     'CHAT_CONVERSATION_CREATE_OWN',
                                                     'CHAT_CONVERSATION_READ_OWN',
                                                     'CHAT_MESSAGE_SEND_OWN',
                                                     'CHAT_MESSAGE_DELETE_OWN',
                                                     'CHAT_ATTACHMENT_CREATE_OWN',
                                                     'CHAT_ATTACHMENT_READ_OWN',
                                                     'CHAT_ATTACHMENT_DELETE_OWN'
                  )
WHERE role_item.code IN ('CUSTOMER', 'EMPLOYEE')
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET
                  is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now();

INSERT INTO iam.role_permissions (id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), role_item.role_id, permission_item.permission_id, TRUE, TRUE
FROM iam.roles role_item
         JOIN iam.permissions permission_item
              ON permission_item.permission_code IN (
                                                     'CHAT_CONVERSATION_CREATE_OWN',
                                                     'CHAT_CONVERSATION_CREATE_ALL',
                                                     'CHAT_CONVERSATION_READ_OWN',
                                                     'CHAT_CONVERSATION_READ_ALL',
                                                     'CHAT_CONVERSATION_UPDATE_ALL',
                                                     'CHAT_CONVERSATION_ASSIGN_ALL',
                                                     'CHAT_MESSAGE_SEND_OWN',
                                                     'CHAT_MESSAGE_DELETE_OWN',
                                                     'CHAT_MESSAGE_MODERATE_ALL',
                                                     'CHAT_ATTACHMENT_CREATE_OWN',
                                                     'CHAT_ATTACHMENT_READ_OWN',
                                                     'CHAT_ATTACHMENT_DELETE_OWN',
                                                     'CHAT_ATTACHMENT_MODERATE_ALL'
                  )
WHERE role_item.code IN ('PARKING_MANAGER', 'SYSTEM_ADMIN')
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET
                  is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now();

COMMIT;
