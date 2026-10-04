CREATE INDEX IF NOT EXISTS idx_partner_registration_applicant_created
    ON operations.approval_requests (requested_by, created_at DESC)
    WHERE request_type = 'PARTNER_REGISTRATION';

CREATE UNIQUE INDEX IF NOT EXISTS uq_partner_registration_pending_applicant
    ON operations.approval_requests (requested_by)
    WHERE request_type = 'PARTNER_REGISTRATION'
      AND status = 'PENDING'
      AND requested_by IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_partner_registration_pending_email
    ON operations.approval_requests (lower(request_data ->> 'email'))
    WHERE request_type = 'PARTNER_REGISTRATION'
      AND status = 'PENDING'
      AND requested_by IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_partner_registration_pending_organization_code
    ON operations.approval_requests (upper(request_data ->> 'organizationCode'))
    WHERE request_type = 'PARTNER_REGISTRATION'
      AND status = 'PENDING'
      AND requested_by IS NOT NULL;
