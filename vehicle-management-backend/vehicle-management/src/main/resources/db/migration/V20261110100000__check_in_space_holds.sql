CREATE TABLE parking.check_in_space_holds (
    hold_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id uuid NOT NULL UNIQUE,
    parking_space_id uuid NOT NULL REFERENCES parking.parking_spaces(parking_space_id) ON DELETE RESTRICT,
    parking_lot_id uuid NOT NULL REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT,
    zone_id uuid NOT NULL REFERENCES parking.zones(zone_id) ON DELETE RESTRICT,
    lane_id uuid NOT NULL REFERENCES parking.lanes(lane_id) ON DELETE RESTRICT,
    card_id uuid REFERENCES access_control.cards(card_id) ON DELETE RESTRICT,
    subscription_id uuid REFERENCES access_control.subscriptions(subscription_id) ON DELETE RESTRICT,
    customer_reservation_id uuid REFERENCES parking.customer_reservations(reservation_id) ON DELETE RESTRICT,
    vehicle_type_id uuid NOT NULL REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT,
    license_plate_normalized varchar(20) NOT NULL,
    credential_hash varchar(64) NOT NULL,
    customer_type varchar(20) NOT NULL,
    operator_account_id uuid NOT NULL REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    status varchar(20) NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_session_id uuid REFERENCES parking.parking_sessions(parking_session_id) ON DELETE RESTRICT,
    cancel_reason varchar(100),
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT ck_check_in_space_holds_status
        CHECK (status IN ('HELD', 'CONSUMED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_check_in_space_holds_customer_type
        CHECK (customer_type IN ('VISITOR', 'SUBSCRIPTION')),
    CONSTRAINT ck_check_in_space_holds_consumed_session
        CHECK (status <> 'CONSUMED' OR consumed_session_id IS NOT NULL)
);

CREATE UNIQUE INDEX uq_check_in_space_holds_held_space
    ON parking.check_in_space_holds(parking_space_id)
    WHERE status = 'HELD';

CREATE UNIQUE INDEX uq_check_in_space_holds_held_card
    ON parking.check_in_space_holds(card_id)
    WHERE status = 'HELD' AND card_id IS NOT NULL;

CREATE UNIQUE INDEX uq_check_in_space_holds_held_plate_lot
    ON parking.check_in_space_holds(parking_lot_id, license_plate_normalized)
    WHERE status = 'HELD';

CREATE INDEX idx_check_in_space_holds_expiry
    ON parking.check_in_space_holds(status, expires_at);

CREATE INDEX idx_check_in_space_holds_zone_space
    ON parking.check_in_space_holds(zone_id, parking_space_id, status);

CREATE INDEX idx_check_in_space_holds_operator
    ON parking.check_in_space_holds(operator_account_id, status);

CREATE TRIGGER trg_check_in_space_holds_set_updated_at
    BEFORE UPDATE ON parking.check_in_space_holds
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
