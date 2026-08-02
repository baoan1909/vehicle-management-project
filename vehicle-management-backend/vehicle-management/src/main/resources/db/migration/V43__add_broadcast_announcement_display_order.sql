ALTER TABLE notification.broadcast_announcements
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 100;

ALTER TABLE notification.broadcast_announcements
    ADD CONSTRAINT ck_broadcast_announcements_display_order
        CHECK (display_order >= 1);

CREATE INDEX IF NOT EXISTS idx_broadcast_announcements_ticker_order
    ON notification.broadcast_announcements(status, enabled, display_order, start_at, published_at);
