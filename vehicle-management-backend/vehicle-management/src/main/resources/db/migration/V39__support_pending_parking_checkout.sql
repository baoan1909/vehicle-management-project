BEGIN;

ALTER TABLE parking.parking_events
    DROP CONSTRAINT IF EXISTS ck_parking_events_type;

ALTER TABLE parking.parking_events
    ADD CONSTRAINT ck_parking_events_type
        CHECK (event_type IN (
            'CHECK_IN',
            'CHECK_OUT_PENDING',
            'CHECK_OUT',
            'MANUAL_REVIEW',
            'BARRIER_OPEN'
        ));

CREATE UNIQUE INDEX IF NOT EXISTS uq_parking_events_pending_checkout
    ON parking.parking_events (parking_session_id)
    WHERE event_type = 'CHECK_OUT_PENDING';

COMMIT;
