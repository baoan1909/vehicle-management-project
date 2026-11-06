-- Renewal payments use their own invoice purpose so settlement can extend the
-- existing subscription without creating a second subscription record.
ALTER TABLE billing.invoices
    DROP CONSTRAINT IF EXISTS ck_invoices_purpose;

ALTER TABLE billing.invoices
    ADD CONSTRAINT ck_invoices_purpose CHECK (purpose IN (
        'PARKING_SESSION',
        'SUBSCRIPTION_PURCHASE',
        'SUBSCRIPTION_RENEWAL',
        'SUBSCRIPTION_CARD_DEPOSIT',
        'LOST_CARD',
        'MANUAL'
    ));
