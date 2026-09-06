-- Complete the long-lived assistant and reusable customer/staff support flow.
-- The migration is non-destructive. Existing legacy rows are never merged or deleted.

UPDATE operations.chat_conversations
SET title = U&'Tr\1EE3 l\00FD h\1ED7 tr\1EE3 CoParking',
    updated_at = now()
WHERE conversation_type = 'ASSISTANT_SUPPORT'
  AND title IS DISTINCT FROM U&'Tr\1EE3 l\00FD h\1ED7 tr\1EE3 CoParking';

DROP INDEX IF EXISTS operations.uq_chat_conversations_active_assistant_customer;
CREATE UNIQUE INDEX uq_chat_conversations_assistant_customer
    ON operations.chat_conversations (customer_id)
    WHERE conversation_type = 'ASSISTANT_SUPPORT';

CREATE UNIQUE INDEX uq_chat_conversations_customer_staff_in_use
    ON operations.chat_conversations (customer_id, assigned_to)
    WHERE conversation_type = 'CUSTOMER_DIRECT'
      AND status <> 'CLOSED'
      AND customer_id IS NOT NULL
      AND assigned_to IS NOT NULL;

-- NOT VALID avoids rewriting or guessing the employee endpoint of legacy rows while still
-- enforcing the invariant for every new/updated CUSTOMER_DIRECT conversation.
ALTER TABLE operations.chat_conversations
    ADD CONSTRAINT ck_customer_direct_pair_fields CHECK (
        conversation_type <> 'CUSTOMER_DIRECT'
        OR (customer_id IS NOT NULL AND assigned_to IS NOT NULL)
    ) NOT VALID;

CREATE UNIQUE INDEX uq_chat_messages_ticket_origin_card
    ON operations.chat_messages (conversation_id, related_id)
    WHERE message_type = 'SUPPORT_REQUEST'
      AND related_schema = 'operations'
      AND related_table = 'support_tickets'
      AND related_id IS NOT NULL;

ALTER TABLE operations.support_tickets
    ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER_PORTAL',
    ADD COLUMN source_conversation_id UUID,
    ADD COLUMN source_message_id UUID,
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN first_responded_at TIMESTAMPTZ;

ALTER TABLE operations.support_tickets
    ADD CONSTRAINT ck_support_tickets_source CHECK (
        source IN ('ASSISTANT_CHAT', 'CUSTOMER_PORTAL', 'EMPLOYEE_CHAT', 'OTHER')
    ),
    ADD CONSTRAINT fk_support_tickets_source_conversation
        FOREIGN KEY (source_conversation_id)
        REFERENCES operations.chat_conversations(conversation_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_support_tickets_source_message
        FOREIGN KEY (source_message_id)
        REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_support_tickets_customer_idempotency
    ON operations.support_tickets (customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_support_tickets_customer_created_at
    ON operations.support_tickets (customer_id, created_at DESC, support_ticket_id DESC);

CREATE INDEX idx_support_tickets_assigned_status
    ON operations.support_tickets (assigned_to, status, created_at DESC);

CREATE INDEX idx_support_ticket_links_ticket_conversation
    ON operations.support_ticket_conversation_links (support_ticket_id, conversation_id, linked_at DESC);

