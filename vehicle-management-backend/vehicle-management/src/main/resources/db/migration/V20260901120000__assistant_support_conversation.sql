-- A personal assistant conversation is intentionally separate from human support chats.
-- It may contain ticket cards, but employees are never added as its members.
ALTER TABLE operations.chat_conversations
    DROP CONSTRAINT IF EXISTS ck_chat_conversations_type;

ALTER TABLE operations.chat_conversations
    ADD CONSTRAINT ck_chat_conversations_type CHECK (
        conversation_type IN (
            'INTERNAL_DIRECT', 'INTERNAL_GROUP', 'CUSTOMER_DIRECT', 'ASSISTANT_SUPPORT',
            'SUPPORT_TICKET', 'PARKING_SESSION', 'BILLING', 'LOST_CARD', 'SYSTEM_DIRECT'
        )
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_conversations_active_assistant_customer
    ON operations.chat_conversations (customer_id)
    WHERE conversation_type = 'ASSISTANT_SUPPORT'
      AND status = 'ACTIVE';

CREATE TABLE operations.support_ticket_conversation_links (
    support_ticket_conversation_link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    support_ticket_id UUID NOT NULL REFERENCES operations.support_tickets(support_ticket_id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    link_reason VARCHAR(20) NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    unlinked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT ck_support_ticket_conversation_links_status CHECK (status IN ('ACTIVE', 'HISTORICAL')),
    CONSTRAINT ck_support_ticket_conversation_links_reason CHECK (link_reason IN ('FIRST_REPLY', 'REASSIGNED', 'REOPENED'))
);

CREATE UNIQUE INDEX uq_support_ticket_conversation_links_active_ticket
    ON operations.support_ticket_conversation_links (support_ticket_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_support_ticket_conversation_links_conversation
    ON operations.support_ticket_conversation_links (conversation_id);

CREATE TRIGGER trg_support_ticket_conversation_links_set_updated_at
    BEFORE UPDATE ON operations.support_ticket_conversation_links
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
