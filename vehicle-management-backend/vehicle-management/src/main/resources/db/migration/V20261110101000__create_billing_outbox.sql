CREATE TABLE billing.outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    last_error VARCHAR(2000),
    correlation_id VARCHAR(100),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_billing_outbox_business_event
        UNIQUE (aggregate_type, aggregate_id, event_type),
    CONSTRAINT ck_billing_outbox_status CHECK (status IN (
        'PENDING', 'PROCESSING', 'RETRY', 'PROCESSED', 'DEAD_LETTER'
    )),
    CONSTRAINT ck_billing_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_billing_outbox_due
    ON billing.outbox_events (status, next_retry_at, created_at)
    WHERE status IN ('PENDING', 'RETRY', 'PROCESSING');
