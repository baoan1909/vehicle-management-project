-- Existing catalog entries remain available at every lot until explicitly disabled.
-- Keep the Partner ownership invariant in the database as well as in the use case.
ALTER TABLE parking.parking_lots
    ADD CONSTRAINT uq_parking_lots_organization_id UNIQUE (organization_id, parking_lot_id);

CREATE TABLE catalog.parking_lot_vehicle_type_exclusions (
    organization_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    vehicle_type_id uuid NOT NULL,
    PRIMARY KEY (parking_lot_id, vehicle_type_id),
    CONSTRAINT fk_lot_vehicle_exclusion_lot FOREIGN KEY (organization_id, parking_lot_id)
        REFERENCES parking.parking_lots (organization_id, parking_lot_id) ON DELETE CASCADE,
    CONSTRAINT fk_lot_vehicle_exclusion_type FOREIGN KEY (organization_id, vehicle_type_id)
        REFERENCES catalog.vehicle_types (organization_id, vehicle_type_id) ON DELETE CASCADE
);

CREATE TABLE catalog.parking_lot_ticket_type_exclusions (
    organization_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    ticket_type_id uuid NOT NULL,
    PRIMARY KEY (parking_lot_id, ticket_type_id),
    CONSTRAINT fk_lot_ticket_exclusion_lot FOREIGN KEY (organization_id, parking_lot_id)
        REFERENCES parking.parking_lots (organization_id, parking_lot_id) ON DELETE CASCADE,
    CONSTRAINT fk_lot_ticket_exclusion_type FOREIGN KEY (organization_id, ticket_type_id)
        REFERENCES catalog.ticket_types (organization_id, ticket_type_id) ON DELETE CASCADE
);
