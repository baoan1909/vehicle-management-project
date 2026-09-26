-- A physical card is an operational asset of one parking lot.  The nullable
-- column preserves untraceable legacy cards while preventing them from leaking
-- into a Partner Admin or Parking Manager scope.
ALTER TABLE access_control.cards
    ADD COLUMN IF NOT EXISTS parking_lot_id UUID;

-- Backfill only when the card has a determinable latest parking session.
WITH latest_card_usage AS (
    SELECT DISTINCT ON (session.card_id)
           session.card_id,
           zone.parking_lot_id
    FROM parking.parking_sessions session
    JOIN parking.zones zone ON zone.zone_id = session.zone_id
    WHERE session.card_id IS NOT NULL
      AND session.zone_id IS NOT NULL
    ORDER BY session.card_id, session.check_in_time DESC NULLS LAST, session.created_at DESC
)
UPDATE access_control.cards card
SET parking_lot_id = latest_card_usage.parking_lot_id
FROM latest_card_usage
WHERE card.card_id = latest_card_usage.card_id
  AND card.parking_lot_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_cards_parking_lot'
          AND conrelid = 'access_control.cards'::regclass
    ) THEN
        ALTER TABLE access_control.cards
            ADD CONSTRAINT fk_cards_parking_lot
            FOREIGN KEY (parking_lot_id)
            REFERENCES parking.parking_lots(parking_lot_id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cards_parking_lot_status
    ON access_control.cards(parking_lot_id, status);

-- System Admin monitors data but must not operate partner-owned card inventory.
DELETE FROM iam.role_permissions rp
USING iam.roles role, iam.permissions permission
WHERE rp.role_id = role.role_id
  AND rp.permission_id = permission.permission_id
  AND role.code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN ('CARD_CREATE_ALL', 'CARD_UPDATE_ALL', 'CARD_DELETE_ALL');
