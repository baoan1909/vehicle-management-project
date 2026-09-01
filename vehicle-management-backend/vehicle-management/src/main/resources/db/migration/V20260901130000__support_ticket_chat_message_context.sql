-- Ticket-scoped messages make a reused customer-staff conversation auditable.
ALTER TABLE operations.chat_messages
    ADD COLUMN IF NOT EXISTS context_ticket_id UUID
        REFERENCES operations.support_tickets(support_ticket_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_chat_messages_context_ticket
    ON operations.chat_messages (context_ticket_id, created_at DESC)
    WHERE deleted = FALSE;

ALTER TABLE operations.support_ticket_conversation_links
    ADD COLUMN IF NOT EXISTS linked_by_account_id UUID
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_support_ticket_conversation_links_active_conversation
    ON operations.support_ticket_conversation_links (conversation_id)
    WHERE status = 'ACTIVE';
