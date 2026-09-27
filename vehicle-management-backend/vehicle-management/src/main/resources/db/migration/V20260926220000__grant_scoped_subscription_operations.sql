-- Operation permissions do not imply access to another Partner's records.
-- SubscriptionAccessGuard resolves the permitted lots from the account's
-- PARKING_SCOPE_PARTNER or PARKING_SCOPE_ASSIGNED permission and membership.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'SUBSCRIPTION_CREATE_ALL', 'SUBSCRIPTION_READ_ALL', 'SUBSCRIPTION_UPDATE_ALL',
    'SUBSCRIPTION_APPROVE_ALL', 'SUBSCRIPTION_REJECT_ALL', 'SUBSCRIPTION_CANCEL_ALL',
    'SUBSCRIPTION_ASSIGN_CARD_ALL', 'SUBSCRIPTION_EXPIRE_ALL'
)
WHERE role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

-- The platform operator may monitor registrations, but cannot operate them.
UPDATE iam.role_permissions role_permission SET is_active = false, updated_at = now()
FROM iam.roles role JOIN iam.permissions permission ON true
WHERE role_permission.role_id = role.role_id
  AND role_permission.permission_id = permission.permission_id
  AND role.code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN (
      'SUBSCRIPTION_CREATE_ALL', 'SUBSCRIPTION_UPDATE_ALL', 'SUBSCRIPTION_DELETE_ALL',
      'SUBSCRIPTION_APPROVE_ALL', 'SUBSCRIPTION_REJECT_ALL', 'SUBSCRIPTION_CANCEL_ALL',
      'SUBSCRIPTION_ASSIGN_CARD_ALL', 'SUBSCRIPTION_EXPIRE_ALL'
  );
