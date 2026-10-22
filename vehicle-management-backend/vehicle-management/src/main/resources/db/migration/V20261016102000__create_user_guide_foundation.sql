CREATE TABLE IF NOT EXISTS notification.user_guide_features (
    feature_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT ck_user_guide_features_code_upper CHECK (code = upper(code)),
    CONSTRAINT fk_user_guide_features_created_by FOREIGN KEY (created_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_guide_features_updated_by FOREIGN KEY (updated_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS notification.user_guides (
    guide_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_id UUID NOT NULL,
    code VARCHAR(120) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    display_type VARCHAR(40) NOT NULL,
    replay_policy VARCHAR(40) NOT NULL,
    context_key VARCHAR(120) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    display_order INTEGER NOT NULL DEFAULT 100,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    current_version INTEGER NOT NULL DEFAULT 0,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_user_guides_feature FOREIGN KEY (feature_id)
        REFERENCES notification.user_guide_features(feature_id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_guides_created_by FOREIGN KEY (created_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_guides_updated_by FOREIGN KEY (updated_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_user_guides_code_upper CHECK (code = upper(code)),
    CONSTRAINT ck_user_guides_context_key_upper CHECK (context_key = upper(context_key)),
    CONSTRAINT ck_user_guides_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_user_guides_display_type CHECK (display_type IN (
        'MODAL', 'BOTTOM_SHEET', 'COACH_MARK', 'TOOLTIP', 'POPOVER', 'BANNER', 'INLINE_CARD', 'CALLOUT'
    )),
    CONSTRAINT ck_user_guides_replay_policy CHECK (replay_policy IN (
        'ONCE_PER_VERSION', 'UNTIL_DISMISSED', 'EVERY_SESSION', 'MANUAL_ONLY'
    )),
    CONSTRAINT ck_user_guides_priority CHECK (priority >= 0),
    CONSTRAINT ck_user_guides_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_user_guides_current_version CHECK (current_version >= 0),
    CONSTRAINT ck_user_guides_schedule CHECK (end_at IS NULL OR end_at >= start_at)
);

CREATE INDEX IF NOT EXISTS idx_user_guides_feature_id
    ON notification.user_guides(feature_id);

CREATE TABLE IF NOT EXISTS notification.user_guide_versions (
    guide_version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guide_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    deep_link VARCHAR(1000),
    audience_mode VARCHAR(30) NOT NULL,
    published_at TIMESTAMPTZ,
    published_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT uq_user_guide_versions_guide_number UNIQUE (guide_id, version_number),
    CONSTRAINT fk_user_guide_versions_guide FOREIGN KEY (guide_id)
        REFERENCES notification.user_guides(guide_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_guide_versions_published_by FOREIGN KEY (published_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_guide_versions_created_by FOREIGN KEY (created_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_guide_versions_updated_by FOREIGN KEY (updated_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_user_guide_versions_number CHECK (version_number >= 1),
    CONSTRAINT ck_user_guide_versions_audience_mode CHECK (audience_mode IN (
        'PUBLIC', 'ALL_AUTHENTICATED', 'ROLE', 'PERMISSION_ANY', 'PERMISSION_ALL'
    ))
);

CREATE TABLE IF NOT EXISTS notification.user_guide_steps (
    step_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guide_version_id UUID NOT NULL,
    display_order INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    media_object_key VARCHAR(500),
    anchor_key VARCHAR(120),
    placement VARCHAR(30),
    collapse_ms INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_guide_steps_version_order UNIQUE (guide_version_id, display_order),
    CONSTRAINT fk_user_guide_steps_version FOREIGN KEY (guide_version_id)
        REFERENCES notification.user_guide_versions(guide_version_id) ON DELETE CASCADE,
    CONSTRAINT ck_user_guide_steps_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_user_guide_steps_collapse_ms CHECK (collapse_ms BETWEEN 0 AND 600000),
    CONSTRAINT ck_user_guide_steps_anchor_key_upper CHECK (anchor_key IS NULL OR anchor_key = upper(anchor_key)),
    CONSTRAINT ck_user_guide_steps_placement CHECK (
        placement IS NULL OR placement IN ('TOP', 'RIGHT', 'BOTTOM', 'LEFT', 'CENTER')
    )
);

CREATE TABLE IF NOT EXISTS notification.user_guide_audiences (
    audience_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guide_version_id UUID NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    role_id UUID,
    permission_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    CONSTRAINT fk_user_guide_audiences_version FOREIGN KEY (guide_version_id)
        REFERENCES notification.user_guide_versions(guide_version_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_guide_audiences_role FOREIGN KEY (role_id)
        REFERENCES iam.roles(role_id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_guide_audiences_permission FOREIGN KEY (permission_id)
        REFERENCES iam.permissions(permission_id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_guide_audiences_created_by FOREIGN KEY (created_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_user_guide_audiences_target_type CHECK (target_type IN ('ROLE', 'PERMISSION')),
    CONSTRAINT ck_user_guide_audiences_target CHECK (
        (target_type = 'ROLE' AND role_id IS NOT NULL AND permission_id IS NULL)
        OR (target_type = 'PERMISSION' AND permission_id IS NOT NULL AND role_id IS NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_guide_audiences_version_role
    ON notification.user_guide_audiences(guide_version_id, role_id)
    WHERE target_type = 'ROLE';

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_guide_audiences_version_permission
    ON notification.user_guide_audiences(guide_version_id, permission_id)
    WHERE target_type = 'PERMISSION';

CREATE TABLE IF NOT EXISTS notification.user_guide_user_states (
    user_guide_state_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    guide_id UUID NOT NULL,
    completed_version INTEGER,
    completed_at TIMESTAMPTZ,
    dismissed_version INTEGER,
    dismissed_at TIMESTAMPTZ,
    last_opened_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT uq_user_guide_user_states_account_guide UNIQUE (account_id, guide_id),
    CONSTRAINT fk_user_guide_user_states_account FOREIGN KEY (account_id)
        REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_guide_user_states_guide FOREIGN KEY (guide_id)
        REFERENCES notification.user_guides(guide_id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_guide_user_states_created_by FOREIGN KEY (created_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_user_guide_user_states_updated_by FOREIGN KEY (updated_by)
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_user_guide_user_states_completed_version CHECK (
        completed_version IS NULL OR completed_version >= 1
    ),
    CONSTRAINT ck_user_guide_user_states_dismissed_version CHECK (
        dismissed_version IS NULL OR dismissed_version >= 1
    ),
    CONSTRAINT ck_user_guide_user_states_completed_pair CHECK (
        (completed_version IS NULL AND completed_at IS NULL)
        OR (completed_version IS NOT NULL AND completed_at IS NOT NULL)
    ),
    CONSTRAINT ck_user_guide_user_states_dismissed_pair CHECK (
        (dismissed_version IS NULL AND dismissed_at IS NULL)
        OR (dismissed_version IS NOT NULL AND dismissed_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_user_guide_user_states_guide_id
    ON notification.user_guide_user_states(guide_id);

