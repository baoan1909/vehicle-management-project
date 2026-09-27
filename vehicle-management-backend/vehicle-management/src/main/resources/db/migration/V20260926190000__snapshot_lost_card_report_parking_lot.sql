-- A report retains the lot where the incident occurred. Card ownership may
-- change later, so the current card lot is not a reliable historical scope.
ALTER TABLE access_control.lost_card_reports
    ADD COLUMN parking_lot_id UUID;

UPDATE access_control.lost_card_reports report
SET parking_lot_id = (
    SELECT session.parking_lot_id FROM parking.parking_sessions session
    WHERE session.parking_session_id = report.parking_session_id
)
WHERE report.parking_session_id IS NOT NULL;

-- Historical outside-parking reports have no immutable lot reference. Keep
-- them unassigned for Partner/Manager access until an operator reconciles them.
ALTER TABLE access_control.lost_card_reports
    ADD CONSTRAINT fk_lost_card_reports_parking_lot
    FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT;

CREATE INDEX idx_lost_card_reports_parking_lot_status
    ON access_control.lost_card_reports (parking_lot_id, status);
