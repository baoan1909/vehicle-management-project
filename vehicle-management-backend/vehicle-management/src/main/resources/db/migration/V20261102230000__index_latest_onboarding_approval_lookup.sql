-- Supports current-access and account lifecycle projections without scanning
-- all approval history for every account. Existing migrations remain immutable.
CREATE INDEX IF NOT EXISTS idx_approval_requests_target_latest
    ON operations.approval_requests (
        request_type,
        target_schema,
        target_table,
        target_id,
        created_at DESC,
        approval_request_id DESC
    );

CREATE INDEX IF NOT EXISTS idx_approval_requests_requester_latest
    ON operations.approval_requests (
        requested_by,
        request_type,
        target_schema,
        target_table,
        created_at DESC,
        approval_request_id DESC
    );
