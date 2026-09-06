-- Keep manager chat capability explicit for databases created before the complete role matrix.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT
    permission_row.id,
    '00000000-0000-0000-0000-000000000005',
    permission_row.permission_id,
    now(), NULL, NULL, NULL, true, true
FROM (
    VALUES
        ('4a7d0500-0000-4000-8000-000000000001'::uuid, '00eaa276-23bc-4fba-bdda-7aab10dd48fe'::uuid),
        ('4a7d0500-0000-4000-8000-000000000002'::uuid, '4123998d-545f-4fad-bb6d-fcdeade1bb80'::uuid),
        ('4a7d0500-0000-4000-8000-000000000003'::uuid, 'f73cd963-66e5-40ba-9905-decd46bc6b15'::uuid)
) AS permission_row(id, permission_id)
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.role_permissions existing_permission
    WHERE existing_permission.role_id = '00000000-0000-0000-0000-000000000005'
      AND existing_permission.permission_id = permission_row.permission_id
);
