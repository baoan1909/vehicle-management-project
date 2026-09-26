-- Partner Admin reviews only Parking Manager onboarding within its own organization.
-- Cross-partner scope is enforced by InternalEmployeeApprovalAccessGuard.
INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    is_active,
    is_system
)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000006',
    '4a7d0300-0000-4000-8000-000000000002',
    now(),
    true,
    true
)
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
