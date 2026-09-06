-- Infer the original support-chat source for legacy tickets whenever an origin card exists.
WITH inferred_origins AS (
    SELECT DISTINCT ON (message.related_id)
           message.related_id AS support_ticket_id,
           message.conversation_id,
           CASE conversation.conversation_type
               WHEN 'ASSISTANT_SUPPORT' THEN 'ASSISTANT_CHAT'
               WHEN 'CUSTOMER_DIRECT' THEN 'EMPLOYEE_CHAT'
               ELSE 'OTHER'
           END AS source
    FROM operations.chat_messages message
    JOIN operations.chat_conversations conversation
      ON conversation.conversation_id = message.conversation_id
    WHERE message.message_type = 'SUPPORT_REQUEST'
      AND message.related_schema = 'operations'
      AND message.related_table = 'support_tickets'
      AND message.related_id IS NOT NULL
    ORDER BY message.related_id, message.created_at, message.message_id
)
UPDATE operations.support_tickets ticket
SET source = origin.source,
    source_conversation_id = origin.conversation_id,
    updated_at = now()
FROM inferred_origins origin
WHERE ticket.support_ticket_id = origin.support_ticket_id
  AND ticket.source_conversation_id IS NULL;

-- New writes were already protected by the NOT VALID check. Validation makes the
-- invariant explicit for the complete legacy dataset without guessing an assignee.
ALTER TABLE operations.chat_conversations
    VALIDATE CONSTRAINT ck_customer_direct_pair_fields;
