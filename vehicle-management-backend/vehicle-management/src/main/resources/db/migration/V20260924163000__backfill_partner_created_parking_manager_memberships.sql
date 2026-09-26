-- A Parking Manager belongs to the Partner Admin who created the account.
-- Backfill accounts created before this relationship was persisted by the application service.
INSERT INTO iam.organization_memberships (
    organization_membership_id,
    organization_id,
    account_id,
    status,
    created_at,
    created_by
)
SELECT DISTINCT
    gen_random_uuid(),
    partner_membership.organization_id,
    manager.account_id,
    'ACTIVE',
    now(),
    manager.created_by
FROM iam.accounts manager
JOIN iam.roles manager_role
    ON manager_role.role_id = manager.role_id
   AND manager_role.code = 'PARKING_MANAGER'
JOIN iam.accounts partner_admin
    ON partner_admin.account_id = manager.created_by
JOIN iam.roles partner_admin_role
    ON partner_admin_role.role_id = partner_admin.role_id
   AND partner_admin_role.code = 'PARTNER_ADMIN'
JOIN iam.organization_memberships partner_membership
    ON partner_membership.account_id = partner_admin.account_id
   AND partner_membership.status = 'ACTIVE'
ON CONFLICT (organization_id, account_id) DO NOTHING;
