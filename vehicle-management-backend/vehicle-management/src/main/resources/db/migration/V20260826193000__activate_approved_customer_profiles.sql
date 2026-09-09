-- A customer approved through onboarding is eligible for self-service only while ACTIVE.
-- Repair legacy approved customer records that were left INACTIVE.
UPDATE people.customers
SET status = 'ACTIVE',
    updated_at = now()
WHERE approval_status = 'APPROVED'
  AND status = 'INACTIVE';

-- Keep the related login account active as well, and retain an audit entry.
INSERT INTO iam.account_status_history (
    account_status_history_id,
    account_id,
    old_status,
    new_status,
    reason,
    changed_at,
    changed_by
)
SELECT
    gen_random_uuid(),
    account.account_id,
    account.status,
    'ACTIVE',
    'Customer onboarding approved',
    now(),
    customer.approved_by
FROM iam.accounts account
JOIN people.customers customer
    ON customer.user_profile_id = account.user_profile_id
WHERE account.status = 'PENDING'
  AND customer.approval_status = 'APPROVED'
  AND customer.status = 'ACTIVE';

UPDATE iam.accounts account
SET status = 'ACTIVE',
    updated_at = now()
FROM people.customers customer
WHERE customer.user_profile_id = account.user_profile_id
  AND account.status = 'PENDING'
  AND customer.approval_status = 'APPROVED'
  AND customer.status = 'ACTIVE';
