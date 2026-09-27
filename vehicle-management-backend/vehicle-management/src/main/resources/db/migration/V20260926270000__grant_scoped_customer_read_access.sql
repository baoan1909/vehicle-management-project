-- Customer identities and their vehicles belong to the platform. Partner operators
-- may only inspect customers/vehicles linked to a lot in their effective scope.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'CUSTOMER_READ_ALL', 'CUSTOMER_VEHICLE_READ_ALL'
)
WHERE role.code IN ('SYSTEM_ADMIN', 'PARTNER_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

-- Global customer profiles remain a platform responsibility, not a parking-lot operation.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'CUSTOMER_UPDATE_ALL', 'CUSTOMER_VEHICLE_CREATE_ALL',
    'CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_DELETE_ALL'
)
WHERE role.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

UPDATE iam.role_permissions role_permission SET is_active = false, updated_at = now()
FROM iam.roles role JOIN iam.permissions permission ON true
WHERE role_permission.role_id = role.role_id
  AND role_permission.permission_id = permission.permission_id
  AND role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
  AND permission.permission_code IN (
      'CUSTOMER_CREATE_ALL', 'CUSTOMER_UPDATE_ALL', 'CUSTOMER_DELETE_ALL',
      'CUSTOMER_VEHICLE_CREATE_ALL', 'CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_DELETE_ALL'
  );
