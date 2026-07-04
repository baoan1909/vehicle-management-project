INSERT INTO people.user_profile_avatars (
    avatar_id,
    user_profile_id,
    object_key,
    bucket,
    status,
    is_current,
    uploaded_by_account_id,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    gen_random_uuid(),
    user_profile_id,
    avatar_url,
    'PUBLIC',
    'ACTIVE',
    true,
    NULL,
    COALESCE(updated_at, created_at, now()),
    NULL,
    NULL,
    NULL
FROM people.user_profiles profile
WHERE profile.avatar_url IS NOT NULL
  AND btrim(profile.avatar_url) <> ''
  AND profile.avatar_url ~ '^av/[0-9]{4}/[0-9]{2}/[0-9]{2}/[0-9a-fA-F-]{36}/pb-[0-9a-fA-F-]{36}-avatar\.[A-Za-z0-9]+$'
  AND NOT EXISTS (
      SELECT 1
      FROM people.user_profile_avatars avatar
      WHERE avatar.user_profile_id = profile.user_profile_id
        AND avatar.is_current = true
  );
