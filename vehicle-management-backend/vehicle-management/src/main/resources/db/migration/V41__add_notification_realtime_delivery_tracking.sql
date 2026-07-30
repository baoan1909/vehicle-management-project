ALTER TABLE notification.notifications
    ADD COLUMN realtime_delivered_at TIMESTAMPTZ;

CREATE INDEX idx_notifications_realtime_pending
    ON notification.notifications(account_id, created_at)
    WHERE channel = 'WEB' AND status = 'SENT' AND read_at IS NULL AND realtime_delivered_at IS NULL;
