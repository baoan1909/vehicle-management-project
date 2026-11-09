ALTER TABLE billing.payments DROP CONSTRAINT IF EXISTS ck_payments_method;
ALTER TABLE billing.payments ADD CONSTRAINT ck_payments_method CHECK (payment_method IN (
    'CASH', 'QR', 'BANK_TRANSFER', 'MOMO', 'VNPAY', 'WALLET', 'PAYOS', 'STRIPE'
));
