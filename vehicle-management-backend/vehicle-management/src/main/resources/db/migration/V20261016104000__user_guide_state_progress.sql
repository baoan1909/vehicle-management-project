-- User Guide runtime progress tracking (Phase 2).
-- Adds nullable progress columns to notification.user_guide_user_states so the
-- backend can resume in-progress guides at the correct step. Nullable: existing
-- rows are unaffected and no backfill is required.

ALTER TABLE notification.user_guide_user_states
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS current_step_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_user_guide_user_states_current_step'
          AND conrelid = 'notification.user_guide_user_states'::regclass
    ) THEN
        ALTER TABLE notification.user_guide_user_states
            ADD CONSTRAINT fk_user_guide_user_states_current_step
                FOREIGN KEY (current_step_id)
                REFERENCES notification.user_guide_steps(step_id)
                ON DELETE SET NULL;
    END IF;
END $$;


