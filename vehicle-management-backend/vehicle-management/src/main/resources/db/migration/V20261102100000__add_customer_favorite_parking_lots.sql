CREATE TABLE IF NOT EXISTS people.customer_favorite_parking_lots (
    customer_id UUID NOT NULL,
    parking_lot_id UUID NOT NULL,
    is_favorite BOOLEAN NOT NULL DEFAULT TRUE,
    favorited_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    unfavorited_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT pk_customer_favorite_parking_lots PRIMARY KEY (customer_id, parking_lot_id),
    CONSTRAINT fk_customer_favorite_parking_lots_customer
        FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_favorite_parking_lots_parking_lot
        FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_customer_favorite_parking_lots_active_customer
    ON people.customer_favorite_parking_lots (customer_id, favorited_at DESC)
    WHERE is_favorite = TRUE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_customer_favorite_parking_lots_set_updated_at'
          AND tgrelid = 'people.customer_favorite_parking_lots'::regclass
    ) THEN
        CREATE TRIGGER trg_customer_favorite_parking_lots_set_updated_at
            BEFORE UPDATE ON people.customer_favorite_parking_lots
            FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
    END IF;
END $$;

