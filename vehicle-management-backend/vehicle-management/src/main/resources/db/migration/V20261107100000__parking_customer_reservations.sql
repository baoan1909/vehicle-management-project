CREATE TABLE parking.customer_reservations (
    reservation_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES people.customers(customer_id),
    parking_lot_id uuid NOT NULL REFERENCES parking.parking_lots(parking_lot_id),
    zone_id uuid NOT NULL REFERENCES parking.zones(zone_id),
    parking_space_id uuid REFERENCES parking.parking_spaces(parking_space_id),
    vehicle_type_id uuid NOT NULL REFERENCES catalog.vehicle_types(vehicle_type_id),
    license_plate varchar(20) NOT NULL,
    qr_token varchar(43) NOT NULL UNIQUE,
    invoice_id uuid NOT NULL UNIQUE REFERENCES billing.invoices(invoice_id),
    parking_session_id uuid UNIQUE REFERENCES parking.parking_sessions(parking_session_id),
    status varchar(24) NOT NULL,
    payment_hold_expires_at timestamptz NOT NULL,
    paid_at timestamptz,
    check_in_deadline timestamptz,
    checked_in_at timestamptz,
    closed_at timestamptz,
    prepaid_amount numeric(12,2) NOT NULL,
    day_price numeric(12,2) NOT NULL,
    night_price numeric(12,2) NOT NULL,
    day_start time NOT NULL,
    day_end time NOT NULL,
    policy_version varchar(20) NOT NULL DEFAULT '1',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz,
    CONSTRAINT ck_customer_reservations_status CHECK (status IN
        ('PENDING_PAYMENT', 'HELD', 'CHECKED_IN', 'CANCELLED', 'EXPIRED', 'PAYMENT_REVIEW')),
    CONSTRAINT ck_customer_reservations_prepaid CHECK (prepaid_amount > 0)
);

CREATE UNIQUE INDEX uq_customer_reservations_active_space
    ON parking.customer_reservations(parking_space_id)
    WHERE parking_space_id IS NOT NULL AND status IN ('PENDING_PAYMENT', 'HELD');
CREATE INDEX idx_customer_reservations_zone_active
    ON parking.customer_reservations(zone_id, status, payment_hold_expires_at, check_in_deadline);
CREATE INDEX idx_customer_reservations_plate_active
    ON parking.customer_reservations(parking_lot_id, license_plate, status);

ALTER TABLE billing.invoices DROP CONSTRAINT IF EXISTS ck_invoices_purpose;
ALTER TABLE billing.invoices ADD CONSTRAINT ck_invoices_purpose CHECK (purpose IN (
    'PARKING_SESSION', 'SUBSCRIPTION_PURCHASE', 'SUBSCRIPTION_RENEWAL',
    'SUBSCRIPTION_CARD_DEPOSIT', 'LOST_CARD', 'MANUAL', 'RESERVATION_PREPAYMENT'
));

ALTER TABLE billing.payments DROP CONSTRAINT IF EXISTS ck_payments_method;
ALTER TABLE billing.payments ADD CONSTRAINT ck_payments_method CHECK (payment_method IN (
    'CASH', 'QR', 'BANK_TRANSFER', 'MOMO', 'VNPAY', 'WALLET', 'PAYOS'
));
