-- Customer "own" subscription reads/creates must not rely on a browser-supplied
-- customerId. This migration adds an idempotency key so retried "register ticket"
-- requests (double click, timeout retry, page reload) return the original row
-- instead of creating duplicates. Mirrors operations.support_tickets handling.

ALTER TABLE access_control.subscriptions
    ADD COLUMN idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX uq_subscriptions_customer_idempotency
    ON access_control.subscriptions (customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_subscriptions_customer_effective_from
    ON access_control.subscriptions (customer_id, effective_from DESC, subscription_id DESC);
