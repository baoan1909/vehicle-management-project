-- Backfill partner personal data from approval request_data into user_profiles,
-- then strip legacy personal keys from request_data.
--
-- Context: PARTNER_REGISTRATION request_data used to duplicate personal data
-- (fullName, username, email, phoneNumber) plus representative fields.
-- Personal data now lives in people.user_profiles (full_name, phone_number);
-- request_data keeps only the organization draft (organizationCode,
-- organizationName, organizationAddress*).
--
-- Safety rules:
-- 1. Backfill full_name/phone_number ONLY when the profile value is blank.
-- 2. Phone fallback order: phoneNumber, then representativePhoneNumber.
-- 3. Abort loudly on phone conflicts instead of silently overwriting data.
-- 4. Never delete approval request rows (PENDING/APPROVED/REJECTED preserved).
-- 5. Organization draft keys and address keys are preserved.

-- Step 1: backfill missing full_name (full_name has no unique constraint).
UPDATE people.user_profiles AS up
SET full_name = candidates.full_name
FROM (
    SELECT DISTINCT ON (a.user_profile_id)
        a.user_profile_id AS user_profile_id,
        NULLIF(btrim(ar.request_data ->> 'fullName'), '') AS full_name
    FROM operations.approval_requests AS ar
    JOIN iam.accounts AS a ON a.account_id = ar.requested_by
    WHERE ar.request_type = 'PARTNER_REGISTRATION'
      AND a.user_profile_id IS NOT NULL
      AND NULLIF(btrim(ar.request_data ->> 'fullName'), '') IS NOT NULL
    ORDER BY a.user_profile_id, ar.created_at DESC
) AS candidates
WHERE up.user_profile_id = candidates.user_profile_id
  AND NULLIF(btrim(up.full_name), '') IS NULL;

-- Step 2: fail fast on phone conflicts instead of silently losing data.
DO $$
DECLARE
    conflict_count integer;
    conflict_sample text;
BEGIN
    SELECT count(*), string_agg(DISTINCT candidates.phone_number, ', ')
    INTO conflict_count, conflict_sample
    FROM (
        SELECT DISTINCT ON (a.user_profile_id)
            a.user_profile_id AS user_profile_id,
            COALESCE(
                NULLIF(btrim(ar.request_data ->> 'phoneNumber'), ''),
                NULLIF(btrim(ar.request_data ->> 'representativePhoneNumber'), '')
            ) AS phone_number
        FROM operations.approval_requests AS ar
        JOIN iam.accounts AS a ON a.account_id = ar.requested_by
        WHERE ar.request_type = 'PARTNER_REGISTRATION'
          AND a.user_profile_id IS NOT NULL
    ) AS candidates
    JOIN people.user_profiles AS up ON up.user_profile_id = candidates.user_profile_id
    WHERE candidates.phone_number IS NOT NULL
      AND NULLIF(btrim(up.phone_number), '') IS NULL
      AND EXISTS (
          SELECT 1
          FROM people.user_profiles AS other
          WHERE other.user_profile_id <> candidates.user_profile_id
            AND other.phone_number = candidates.phone_number
      );

    IF conflict_count > 0 THEN
        RAISE EXCEPTION 'Partner phone backfill collides with existing profiles (count=%, phones=%)',
            conflict_count, conflict_sample;
    END IF;
END
$$;

-- Step 3: backfill missing phone_number (verified conflict-free above).
UPDATE people.user_profiles AS up
SET phone_number = candidates.phone_number
FROM (
    SELECT DISTINCT ON (a.user_profile_id)
        a.user_profile_id AS user_profile_id,
        COALESCE(
            NULLIF(btrim(ar.request_data ->> 'phoneNumber'), ''),
            NULLIF(btrim(ar.request_data ->> 'representativePhoneNumber'), '')
        ) AS phone_number
    FROM operations.approval_requests AS ar
    JOIN iam.accounts AS a ON a.account_id = ar.requested_by
    WHERE ar.request_type = 'PARTNER_REGISTRATION'
      AND a.user_profile_id IS NOT NULL
    ORDER BY a.user_profile_id, ar.created_at DESC
) AS candidates
WHERE up.user_profile_id = candidates.user_profile_id
  AND candidates.phone_number IS NOT NULL
  AND NULLIF(btrim(up.phone_number), '') IS NULL;

-- Step 4: strip legacy personal keys. Organization draft keys
-- (organizationCode, organizationName, organizationAddress*) are preserved.
UPDATE operations.approval_requests
SET request_data = request_data - ARRAY[
    'fullName',
    'username',
    'email',
    'phoneNumber',
    'representativeName',
    'representativePhoneNumber'
]
WHERE request_type = 'PARTNER_REGISTRATION';

-- Step 5: report remaining rows per status for operator verification.
DO $$
DECLARE
    pending_count integer;
    approved_count integer;
    rejected_count integer;
BEGIN
    SELECT count(*) INTO pending_count
    FROM operations.approval_requests
    WHERE request_type = 'PARTNER_REGISTRATION' AND status = 'PENDING';
    SELECT count(*) INTO approved_count
    FROM operations.approval_requests
    WHERE request_type = 'PARTNER_REGISTRATION' AND status = 'APPROVED';
    SELECT count(*) INTO rejected_count
    FROM operations.approval_requests
    WHERE request_type = 'PARTNER_REGISTRATION' AND status = 'REJECTED';
    RAISE NOTICE 'Partner request_data cleanup done. PENDING=%, APPROVED=%, REJECTED=%',
        pending_count, approved_count, rejected_count;
END
$$;
