-- User Guide runtime progress tracking (Phase 2).
-- Adds nullable progress columns to notification.user_guide_user_states so the
-- backend can resume in-progress guides at the correct step. Nullable: existing
-- rows are unaffected and no backfill is required.

ALTER TABLE notification.user_guide_user_states
    ADD COLUMN started_at TIMESTAMPTZ,
    ADD COLUMN current_step_id UUID;

ALTER TABLE notification.user_guide_user_states
    ADD CONSTRAINT fk_user_guide_user_states_current_step
        FOREIGN KEY (current_step_id)
        REFERENCES notification.user_guide_steps(step_id)
        ON DELETE SET NULL;


