ALTER TABLE operations.support_tickets
    ADD COLUMN IF NOT EXISTS last_context_activity_at timestamptz,
    ADD COLUMN IF NOT EXISTS auto_close_at timestamptz,
    ADD COLUMN IF NOT EXISTS close_reason varchar(30);

UPDATE operations.support_tickets
SET auto_close_at = resolved_at + interval '24 hours'
WHERE status = 'RESOLVED'
  AND resolved_at IS NOT NULL
  AND auto_close_at IS NULL;

UPDATE operations.support_tickets
SET close_reason = CASE
        WHEN closed_by IS NULL THEN 'SYSTEM_MAINTENANCE'
        ELSE 'MANUAL'
    END
WHERE status = 'CLOSED'
  AND close_reason IS NULL;

ALTER TABLE operations.support_tickets
    DROP CONSTRAINT IF EXISTS ck_support_tickets_close_reason,
    ADD CONSTRAINT ck_support_tickets_close_reason
        CHECK (close_reason IS NULL OR close_reason IN ('MANUAL', 'INACTIVITY', 'SYSTEM_MAINTENANCE')),
    DROP CONSTRAINT IF EXISTS ck_support_tickets_auto_close_state,
    ADD CONSTRAINT ck_support_tickets_auto_close_state
        CHECK (
            (status = 'RESOLVED' AND auto_close_at IS NOT NULL)
            OR (status <> 'RESOLVED' AND auto_close_at IS NULL)
        ),
    DROP CONSTRAINT IF EXISTS ck_support_tickets_closed_reason_state,
    ADD CONSTRAINT ck_support_tickets_closed_reason_state
        CHECK (
            (status = 'CLOSED' AND close_reason IS NOT NULL)
            OR (status <> 'CLOSED' AND close_reason IS NULL)
        );

CREATE INDEX IF NOT EXISTS idx_support_tickets_auto_close_due
    ON operations.support_tickets (auto_close_at, support_ticket_id)
    WHERE status = 'RESOLVED';

CREATE INDEX IF NOT EXISTS idx_chat_messages_context_activity
    ON operations.chat_messages (context_ticket_id, created_at DESC)
    WHERE context_ticket_id IS NOT NULL;
