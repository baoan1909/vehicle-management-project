BEGIN;

WITH target_permissions(permission_code) AS (
    VALUES
        ('DASHBOARD_READ_ALL'),
        ('PARKING_SESSION_CHECK_OUT_ALL'),
        ('SUPPORT_TICKET_READ_ALL'),
        ('SUPPORT_TICKET_ASSIGN'),
        ('SUPPORT_TICKET_PROCESS_ALL'),
        ('SUBSCRIPTION_READ_ALL'),
        ('SUBSCRIPTION_APPROVE_ALL'),
        ('SUBSCRIPTION_REJECT_ALL'),
        ('SUBSCRIPTION_ASSIGN_CARD_ALL'),
        ('SHIFT_READ_ALL'),
        ('SHIFT_ASSIGNMENT_READ_ALL'),
        ('LOST_CARD_REPORT_READ_ALL')
),
target_role_permissions AS (
    SELECT role_permission.id
    FROM iam.role_permissions role_permission
             JOIN iam.roles role_item
                  ON role_item.role_id = role_permission.role_id
             JOIN iam.permissions permission_item
                  ON permission_item.permission_id = role_permission.permission_id
             JOIN target_permissions target_permission
                  ON target_permission.permission_code = permission_item.permission_code
    WHERE role_item.code = 'SYSTEM_ADMIN'
      AND role_permission.is_active = TRUE
)
UPDATE iam.role_permissions role_permission
SET is_active = FALSE,
    updated_at = now()
WHERE role_permission.id IN (
    SELECT id
    FROM target_role_permissions
);

COMMIT;
