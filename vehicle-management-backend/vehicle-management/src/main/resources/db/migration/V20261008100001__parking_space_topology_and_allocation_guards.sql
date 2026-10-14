-- =========================================================
-- PHASE 1 HARDENING: topology invariants, single draft/published,
-- session-space FK, allocation overlap exclusion
-- =========================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 1. FK from open sessions to the occupying space.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_sessions_space') THEN
        ALTER TABLE parking.parking_sessions
            ADD CONSTRAINT fk_parking_sessions_space
            FOREIGN KEY (parking_space_id) REFERENCES parking.parking_spaces(parking_space_id) ON DELETE SET NULL;
    END IF;
END
$$;

-- 2. A zone must reference a level of the same parking lot.
CREATE OR REPLACE FUNCTION parking.check_zone_level_topology()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parking_level_id IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM parking.parking_levels pl
            WHERE pl.parking_level_id = NEW.parking_level_id
              AND pl.parking_lot_id = NEW.parking_lot_id
        ) THEN
        RAISE EXCEPTION 'Zone % references level % of another parking lot', NEW.zone_id, NEW.parking_level_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_zones_check_level_topology ON parking.zones;
CREATE TRIGGER trg_zones_check_level_topology
    BEFORE INSERT OR UPDATE OF parking_level_id, parking_lot_id ON parking.zones
    FOR EACH ROW EXECUTE FUNCTION parking.check_zone_level_topology();

-- 3. A layout item must belong to the version's zone.
CREATE OR REPLACE FUNCTION parking.check_layout_item_topology()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM parking.parking_layout_versions lv
        JOIN parking.parking_spaces ps ON ps.parking_space_id = NEW.parking_space_id
        WHERE lv.layout_version_id = NEW.layout_version_id
          AND ps.zone_id = lv.zone_id
    ) THEN
        RAISE EXCEPTION 'Layout item % references a space outside the version zone', NEW.layout_item_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_layout_items_check_topology ON parking.parking_space_layout_items;
CREATE TRIGGER trg_layout_items_check_topology
    BEFORE INSERT OR UPDATE OF layout_version_id, parking_space_id ON parking.parking_space_layout_items
    FOR EACH ROW EXECUTE FUNCTION parking.check_layout_item_topology();

-- 4. At most one DRAFT and one PUBLISHED layout per zone.
CREATE UNIQUE INDEX IF NOT EXISTS uq_layout_one_draft_per_zone
    ON parking.parking_layout_versions (zone_id)
    WHERE status = 'DRAFT';
CREATE UNIQUE INDEX IF NOT EXISTS uq_layout_one_published_per_zone
    ON parking.parking_layout_versions (zone_id)
    WHERE status = 'PUBLISHED';

-- 5. Replace the exact-date unique constraint with a true overlap guard:
-- no two HELD/ACTIVE allocations for the same space may cover intersecting dates.
ALTER TABLE parking.parking_space_allocations
    DROP CONSTRAINT IF EXISTS uq_parking_space_allocations_space_time;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ex_parking_space_allocations_no_overlap') THEN
        ALTER TABLE parking.parking_space_allocations
            ADD CONSTRAINT ex_parking_space_allocations_no_overlap
            EXCLUDE USING gist (
                parking_space_id WITH =,
                daterange(effective_from, effective_to, '[]') WITH &&
            )
            WHERE (allocation_status IN ('HELD', 'ACTIVE'));
    END IF;
END
$$;

-- 6. Complete geometry constraints and history-safe allocation deletes.
ALTER TABLE parking.parking_levels ADD CONSTRAINT ck_parking_levels_floor_height_positive
        CHECK (floor_height IS NULL OR floor_height > 0);

ALTER TABLE parking.parking_spaces
    ADD CONSTRAINT ck_parking_spaces_geometry_complete
        CHECK ((x IS NULL AND y IS NULL AND width IS NULL AND height IS NULL)
            OR (x IS NOT NULL AND y IS NOT NULL AND width IS NOT NULL AND height IS NOT NULL)),
    ADD CONSTRAINT ck_parking_spaces_geometry_non_negative
        CHECK ((x IS NULL OR x >= 0) AND (y IS NULL OR y >= 0)),
    ADD CONSTRAINT ck_parking_spaces_rotation_range
        CHECK (rotation >= 0 AND rotation < 360);

ALTER TABLE parking.parking_space_layout_items
    ADD CONSTRAINT ck_parking_space_layout_items_position_non_negative CHECK (x >= 0 AND y >= 0),
    ADD CONSTRAINT ck_parking_space_layout_items_rotation_range CHECK (rotation >= 0 AND rotation < 360);

CREATE TRIGGER trg_parking_spaces_set_updated_at
    BEFORE UPDATE ON parking.parking_spaces
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE parking.parking_space_allocations
    DROP CONSTRAINT fk_parking_space_allocations_space,
    DROP CONSTRAINT fk_parking_space_allocations_subscription,
    ADD CONSTRAINT fk_parking_space_allocations_space
        FOREIGN KEY (parking_space_id) REFERENCES parking.parking_spaces(parking_space_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_parking_space_allocations_subscription
        FOREIGN KEY (subscription_id) REFERENCES access_control.subscriptions(subscription_id) ON DELETE RESTRICT;

ALTER TABLE parking.parking_space_layout_items
    DROP CONSTRAINT fk_parking_space_layout_items_space,
    ADD CONSTRAINT fk_parking_space_layout_items_space
        FOREIGN KEY (parking_space_id) REFERENCES parking.parking_spaces(parking_space_id)
        ON DELETE RESTRICT;

-- 7. An allocation must connect a subscription and a space from the same lot.
CREATE OR REPLACE FUNCTION parking.check_space_allocation_topology()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM parking.parking_spaces ps
        JOIN parking.zones z ON z.zone_id = ps.zone_id
        JOIN access_control.subscriptions s ON s.subscription_id = NEW.subscription_id
        WHERE ps.parking_space_id = NEW.parking_space_id
          AND s.parking_lot_id IS NOT NULL
          AND s.parking_lot_id = z.parking_lot_id
    ) THEN
        RAISE EXCEPTION 'Allocation space and subscription must belong to the same parking lot';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_space_allocations_check_topology
    BEFORE INSERT OR UPDATE OF parking_space_id, subscription_id
    ON parking.parking_space_allocations
    FOR EACH ROW EXECUTE FUNCTION parking.check_space_allocation_topology();

-- 8. Session, space and optional subscription must resolve to the same lot.
CREATE OR REPLACE FUNCTION parking.check_session_space_topology()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parking_space_id IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM parking.parking_spaces ps
        JOIN parking.zones z ON z.zone_id = ps.zone_id
        WHERE ps.parking_space_id = NEW.parking_space_id
          AND NEW.parking_lot_id IS NOT NULL
          AND z.parking_lot_id = NEW.parking_lot_id
          AND NEW.zone_id IS NOT NULL
          AND ps.zone_id = NEW.zone_id
    ) THEN
        RAISE EXCEPTION 'Parking session space must belong to the session zone and parking lot';
    END IF;

    IF NEW.subscription_id IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM access_control.subscriptions s
        WHERE s.subscription_id = NEW.subscription_id
          AND s.parking_lot_id IS NOT NULL
          AND NEW.parking_lot_id IS NOT NULL
          AND s.parking_lot_id = NEW.parking_lot_id
    ) THEN
        RAISE EXCEPTION 'Parking session subscription must belong to the session parking lot';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_parking_sessions_check_space_topology
    BEFORE INSERT OR UPDATE OF parking_space_id, subscription_id, zone_id, parking_lot_id
    ON parking.parking_sessions
    FOR EACH ROW EXECUTE FUNCTION parking.check_session_space_topology();

-- 9. Moving an already referenced space would invalidate layout/history topology.
CREATE OR REPLACE FUNCTION parking.prevent_referenced_space_zone_move()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.zone_id IS DISTINCT FROM OLD.zone_id
       AND (EXISTS (SELECT 1 FROM parking.parking_space_layout_items WHERE parking_space_id = OLD.parking_space_id)
         OR EXISTS (SELECT 1 FROM parking.parking_space_allocations WHERE parking_space_id = OLD.parking_space_id)
         OR EXISTS (SELECT 1 FROM parking.parking_sessions WHERE parking_space_id = OLD.parking_space_id)) THEN
        RAISE EXCEPTION 'Referenced parking space cannot be moved to another zone';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_parking_spaces_prevent_referenced_zone_move
    BEFORE UPDATE OF zone_id ON parking.parking_spaces
    FOR EACH ROW EXECUTE FUNCTION parking.prevent_referenced_space_zone_move();
