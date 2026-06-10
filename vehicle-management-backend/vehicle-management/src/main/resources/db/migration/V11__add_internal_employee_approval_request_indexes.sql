CREATE INDEX IF NOT EXISTS idx_approval_requests_request_type_status
    ON operations.approval_requests(request_type, status);

CREATE INDEX IF NOT EXISTS idx_approval_requests_target_lookup
    ON operations.approval_requests(target_schema, target_table, target_id);
