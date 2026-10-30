ALTER TABLE parking.parking_sessions
    ALTER COLUMN card_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_parking_sessions_open_subscription
    ON parking.parking_sessions(subscription_id)
    WHERE status = 'OPEN' AND subscription_id IS NOT NULL;

COMMENT ON COLUMN parking.parking_sessions.card_id IS
    'Physical card used for the session; nullable when a subscription QR credential is used.';
