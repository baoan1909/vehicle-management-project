ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS qr_access_token VARCHAR(64),
    ADD COLUMN IF NOT EXISTS qr_issued_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS qr_revoked_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscriptions_qr_access_token
    ON access_control.subscriptions (qr_access_token)
    WHERE qr_access_token IS NOT NULL;

COMMENT ON COLUMN access_control.subscriptions.qr_access_token IS
    'Opaque random credential encoded in the customer subscription QR code.';
COMMENT ON COLUMN access_control.subscriptions.qr_issued_at IS
    'Time at which the current QR credential was issued.';
COMMENT ON COLUMN access_control.subscriptions.qr_revoked_at IS
    'Time at which the QR credential was revoked; null while usable.';
