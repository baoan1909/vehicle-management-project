-- Organization profile updates are no longer part of the supported Partner Admin workflow.
-- Keep the historical permission definitions for audit compatibility, but remove their role
-- assignments so the old API endpoints cannot be used with a direct request.
DELETE FROM iam.role_permissions role_permission
USING iam.roles role, iam.permissions permission
WHERE role_permission.role_id = role.role_id
  AND role_permission.permission_id = permission.permission_id
  AND (
      (role.code = 'PARTNER_ADMIN'
          AND permission.permission_code IN (
              'ORGANIZATION_PROFILE_READ_OWN',
              'ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN'
          ))
      OR (role.code = 'SYSTEM_ADMIN'
          AND permission.permission_code = 'ORGANIZATION_PROFILE_REVIEW_ALL')
  );
