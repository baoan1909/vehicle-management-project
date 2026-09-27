-- Keep the physical origin of a bill even if the related source changes later.
ALTER TABLE billing.invoices ADD COLUMN parking_lot_id uuid;

UPDATE billing.invoices invoice
SET parking_lot_id = session.parking_lot_id
FROM parking.parking_sessions session
WHERE invoice.parking_session_id = session.parking_session_id
  AND invoice.parking_lot_id IS NULL;

UPDATE billing.invoices invoice
SET parking_lot_id = subscription.parking_lot_id
FROM access_control.subscriptions subscription
WHERE invoice.subscription_id = subscription.subscription_id
  AND invoice.parking_lot_id IS NULL;

UPDATE billing.invoices invoice
SET parking_lot_id = report.parking_lot_id
FROM access_control.lost_card_reports report
WHERE invoice.lost_card_report_id = report.lost_card_report_id
  AND invoice.parking_lot_id IS NULL;

ALTER TABLE billing.invoices ADD CONSTRAINT fk_invoices_parking_lot
    FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT;
CREATE INDEX idx_invoices_parking_lot_issued_at
    ON billing.invoices (parking_lot_id, issued_at DESC);
