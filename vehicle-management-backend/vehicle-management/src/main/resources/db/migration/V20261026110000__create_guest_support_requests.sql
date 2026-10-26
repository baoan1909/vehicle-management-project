CREATE TABLE operations.guest_support_requests (
    guest_support_request_id UUID PRIMARY KEY,
    public_reference_code VARCHAR(32) NOT NULL,
    category_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(20),
    contact_hash VARCHAR(64) NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    session_hash VARCHAR(64),
    idempotency_hash VARCHAR(64) NOT NULL,
    capability_token_hash VARCHAR(64) NOT NULL,
    capability_expires_at TIMESTAMPTZ NOT NULL,
    retention_expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_guest_support_category
        FOREIGN KEY (category_id) REFERENCES operations.support_ticket_categories(category_id) ON DELETE RESTRICT,
    CONSTRAINT uq_guest_support_reference UNIQUE (public_reference_code),
    CONSTRAINT uq_guest_support_idempotency UNIQUE (contact_hash, idempotency_hash),
    CONSTRAINT ck_guest_support_contact CHECK (email IS NOT NULL OR phone IS NOT NULL),
    CONSTRAINT ck_guest_support_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
);

CREATE INDEX idx_guest_support_ip_created
    ON operations.guest_support_requests (ip_hash, created_at DESC);

CREATE INDEX idx_guest_support_contact_created
    ON operations.guest_support_requests (contact_hash, created_at DESC);

CREATE INDEX idx_guest_support_session_created
    ON operations.guest_support_requests (session_hash, created_at DESC)
    WHERE session_hash IS NOT NULL;

CREATE INDEX idx_guest_support_retention
    ON operations.guest_support_requests (retention_expires_at);
