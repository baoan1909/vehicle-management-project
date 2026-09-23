-- Backfill accounts approved before customer onboarding also activated iam.accounts.
-- Only customer accounts whose approved customer record is already active are affected.
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
