DROP INDEX IF EXISTS access_control.idx_cards_vehicle_type_status;

ALTER TABLE access_control.cards
    DROP CONSTRAINT IF EXISTS fk_cards_vehicle_type;

ALTER TABLE access_control.cards
    DROP COLUMN IF EXISTS vehicle_type_id;
