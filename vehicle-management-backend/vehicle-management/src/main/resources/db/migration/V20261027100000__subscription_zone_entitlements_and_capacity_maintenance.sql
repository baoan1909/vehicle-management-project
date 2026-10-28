-- Zone-scoped subscription entitlements and CAPACITY maintenance controls.
-- Lock order used by application flows: subscription -> zone -> space -> card -> session.

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE parking.zones
    ADD COLUMN unavailable_capacity INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_zones_unavailable_capacity
        CHECK (unavailable_capacity >= 0 AND unavailable_capacity <= capacity);

-- Existing manual holds predate the mandatory expiry invariant. Give them a
-- finite grace period before enforcing it at the database boundary.
UPDATE parking.parking_space_allocations
SET hold_expires_at = now() + INTERVAL '48 hours'
WHERE allocation_status = 'HELD' AND hold_expires_at IS NULL;

ALTER TABLE parking.parking_space_allocations
    ADD CONSTRAINT ck_parking_space_allocations_hold_expiry
        CHECK (allocation_status <> 'HELD' OR hold_expires_at IS NOT NULL);

CREATE TABLE parking.subscription_capacity_allocations (
    allocation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    zone_id UUID NOT NULL,
    vehicle_type_id UUID NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NOT NULL,
    allocation_status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    hold_expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_subscription_capacity_allocations_subscription
        FOREIGN KEY (subscription_id) REFERENCES access_control.subscriptions(subscription_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscription_capacity_allocations_zone
        FOREIGN KEY (zone_id) REFERENCES parking.zones(zone_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscription_capacity_allocations_vehicle_type
        FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT,
    CONSTRAINT ck_subscription_capacity_allocations_status
        CHECK (allocation_status IN ('HELD', 'ACTIVE', 'RELEASED', 'CANCELLED')),
    CONSTRAINT ck_subscription_capacity_allocations_dates CHECK (effective_to >= effective_from),
    CONSTRAINT ck_subscription_capacity_allocations_hold_expiry
        CHECK (allocation_status <> 'HELD' OR hold_expires_at IS NOT NULL)
);

CREATE UNIQUE INDEX uq_subscription_capacity_allocation_live_subscription
    ON parking.subscription_capacity_allocations(subscription_id)
    WHERE allocation_status IN ('HELD', 'ACTIVE');

CREATE INDEX idx_subscription_capacity_allocations_zone_dates
    ON parking.subscription_capacity_allocations(zone_id, effective_from, effective_to)
    WHERE allocation_status IN ('HELD', 'ACTIVE');

CREATE TRIGGER trg_subscription_capacity_allocations_set_updated_at
    BEFORE UPDATE ON parking.subscription_capacity_allocations
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE OR REPLACE FUNCTION parking.check_capacity_allocation_topology()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM access_control.subscriptions s
        JOIN people.customer_vehicles cv ON cv.customer_vehicle_id = s.customer_vehicle_id
        JOIN parking.zones z ON z.zone_id = NEW.zone_id
        JOIN parking.zone_vehicle_types zvt
          ON zvt.zone_id = z.zone_id AND zvt.vehicle_type_id = NEW.vehicle_type_id
        WHERE s.subscription_id = NEW.subscription_id
          AND s.parking_lot_id = z.parking_lot_id
          AND cv.vehicle_type_id = NEW.vehicle_type_id
          AND z.tracking_mode = 'CAPACITY'
    ) THEN
        RAISE EXCEPTION 'Capacity allocation subscription, zone and vehicle type are incompatible';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_capacity_allocations_check_topology
    BEFORE INSERT OR UPDATE OF subscription_id, zone_id, vehicle_type_id
    ON parking.subscription_capacity_allocations
    FOR EACH ROW EXECUTE FUNCTION parking.check_capacity_allocation_topology();

-- Database backstops for duplicate admission. Application validation still
-- translates conflicts to HTTP 409 and supplies the user-facing reason.
CREATE UNIQUE INDEX uq_parking_sessions_open_card
    ON parking.parking_sessions(card_id) WHERE status = 'OPEN';
CREATE UNIQUE INDEX uq_parking_sessions_open_subscription
    ON parking.parking_sessions(subscription_id)
    WHERE status = 'OPEN' AND subscription_id IS NOT NULL;
CREATE UNIQUE INDEX uq_parking_sessions_open_customer_vehicle
    ON parking.parking_sessions(customer_vehicle_id)
    WHERE status = 'OPEN' AND customer_vehicle_id IS NOT NULL;

