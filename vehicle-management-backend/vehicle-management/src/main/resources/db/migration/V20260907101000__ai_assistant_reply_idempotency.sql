CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_messages_ai_reply_input
    ON operations.chat_messages (reply_to_message_id)
    WHERE message_type = 'SYSTEM'
      AND sender_account_id IS NULL
      AND related_schema = 'ai'
      AND related_table = 'assistant_jobs'
      AND reply_to_message_id IS NOT NULL;
