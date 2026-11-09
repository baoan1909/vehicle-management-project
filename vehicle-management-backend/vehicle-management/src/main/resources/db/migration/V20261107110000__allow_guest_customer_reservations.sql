-- Guest reservations are linked to a license plate and a private access token
-- instead of a registered customer account.
ALTER TABLE parking.customer_reservations
    ALTER COLUMN customer_id DROP NOT NULL;
