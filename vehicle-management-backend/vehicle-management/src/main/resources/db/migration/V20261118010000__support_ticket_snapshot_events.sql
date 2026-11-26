CREATE TABLE IF NOT EXISTS operations.support_ticket_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    support_ticket_id UUID NOT NULL,
    conversation_id UUID NOT NULL,
    actor_account_id UUID,
    event_type VARCHAR(30) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    snapshot_status VARCHAR(20) NOT NULL,
    snapshot_payload JSONB NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_support_ticket_events_ticket
        FOREIGN KEY (support_ticket_id) REFERENCES operations.support_tickets(support_ticket_id) ON DELETE CASCADE,
    CONSTRAINT fk_support_ticket_events_conversation
        FOREIGN KEY (conversation_id) REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    CONSTRAINT fk_support_ticket_events_actor
        FOREIGN KEY (actor_account_id) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_support_ticket_events_type CHECK (
        event_type IN ('SHARED', 'STARTED_PROCESSING', 'RESOLVED', 'REOPENED', 'CLOSED', 'LEGACY_IMPORTED')
    ),
    CONSTRAINT ck_support_ticket_events_from_status CHECK (
        from_status IS NULL OR from_status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')
    ),
    CONSTRAINT ck_support_ticket_events_to_status CHECK (
        to_status IS NULL OR to_status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')
    ),
    CONSTRAINT ck_support_ticket_events_snapshot_status CHECK (
        snapshot_status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')
    ),
    CONSTRAINT uq_support_ticket_events_conversation_idempotency UNIQUE (conversation_id, idempotency_key)
);

ALTER TABLE operations.chat_messages
    ADD COLUMN IF NOT EXISTS support_ticket_event_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.chat_messages'::regclass
          AND conname = 'fk_chat_messages_support_ticket_event'
    ) THEN
        ALTER TABLE operations.chat_messages
            ADD CONSTRAINT fk_chat_messages_support_ticket_event
                FOREIGN KEY (support_ticket_event_id)
                REFERENCES operations.support_ticket_events(event_id)
                ON DELETE SET NULL;
    END IF;
END;
$$;

DROP INDEX IF EXISTS operations.uq_chat_messages_ticket_origin_card;

-- Legacy cards did not persist an immutable status. Import them explicitly as inferred
-- snapshots so historical uncertainty is visible instead of silently treated as exact.
INSERT INTO operations.support_ticket_events (
    event_id,
    support_ticket_id,
    conversation_id,
    actor_account_id,
    event_type,
    snapshot_status,
    snapshot_payload,
    idempotency_key,
    created_at,
    created_by
)
SELECT
    message.message_id,
    message.related_id,
    message.conversation_id,
    message.sender_account_id,
    'LEGACY_IMPORTED',
    ticket.status,
    jsonb_build_object(
        'schemaVersion', 1,
        'eventId', message.message_id,
        'eventType', 'LEGACY_IMPORTED',
        'ticketId', ticket.support_ticket_id,
        'ticketCode', 'TK-' || upper(left(ticket.support_ticket_id::text, 8)),
        'title', ticket.title,
        'content', COALESCE(message.content, ticket.content),
        'categoryCode', category.code,
        'categoryName', category.name,
        'priority', category.priority,
        'snapshotStatus', ticket.status,
        'assignedTo', ticket.assigned_to,
        'actorAccountId', message.sender_account_id,
        'occurredAt', message.created_at,
        'legacyInferred', TRUE
    ),
    'legacy:' || message.message_id,
    message.created_at,
    message.created_by
FROM operations.chat_messages message
JOIN operations.support_tickets ticket ON ticket.support_ticket_id = message.related_id
JOIN operations.support_ticket_categories category ON category.category_id = ticket.category_id
WHERE message.message_type = 'SUPPORT_REQUEST'
  AND message.support_ticket_event_id IS NULL
  AND message.related_schema = 'operations'
  AND message.related_table = 'support_tickets'
  AND message.related_id IS NOT NULL
ON CONFLICT DO NOTHING;

UPDATE operations.chat_messages message
SET support_ticket_event_id = message.message_id
WHERE message.message_type = 'SUPPORT_REQUEST'
  AND message.support_ticket_event_id IS NULL
  AND message.related_schema = 'operations'
  AND message.related_table = 'support_tickets'
  AND EXISTS (
      SELECT 1
      FROM operations.support_ticket_events event
      WHERE event.event_id = message.message_id
  );

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_messages_support_ticket_event
    ON operations.chat_messages(support_ticket_event_id)
    WHERE support_ticket_event_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_support_ticket_events_ticket_created
    ON operations.support_ticket_events(support_ticket_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_support_ticket_events_conversation_created
    ON operations.support_ticket_events(conversation_id, created_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgrelid = 'operations.support_ticket_events'::regclass
          AND tgname = 'trg_support_ticket_events_set_updated_at'
    ) THEN
        CREATE TRIGGER trg_support_ticket_events_set_updated_at
            BEFORE UPDATE ON operations.support_ticket_events
            FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
    END IF;
END;
$$;
