-- Partner Admin is the operational fallback when an organization has no active
-- Parking Manager or Employee. OrganizationAccessGuard still limits every
-- parking-session operation to parking lots owned by the partner organization.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id,
       now(), NULL, NULL, NULL, true, true
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'PARKING_SESSION_PLATE_REVIEW_ALL',
        'PARKING_SESSION_PLATE_OVERRIDE_ALL'
    )
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
