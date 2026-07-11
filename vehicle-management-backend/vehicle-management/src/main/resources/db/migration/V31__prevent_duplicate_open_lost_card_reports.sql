CREATE UNIQUE INDEX IF NOT EXISTS ux_lost_card_reports_open_parking_session
    ON access_control.lost_card_reports (parking_session_id)
    WHERE parking_session_id IS NOT NULL
      AND status = 'OPEN';
