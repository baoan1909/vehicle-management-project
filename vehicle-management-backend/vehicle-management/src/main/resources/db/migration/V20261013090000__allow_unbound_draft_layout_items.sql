-- Draft layout items are snapshots and intentionally remain unbound from
-- operational parking_spaces until publish materializes them. Keep enforcing
-- zone topology as soon as a parking_space_id is assigned.
CREATE OR REPLACE FUNCTION parking.check_layout_item_topology()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parking_space_id IS NOT NULL
        AND NOT EXISTS (
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
