-- TIMESTAMPTZ values remain the same absolute instants. This migration only
-- changes how new PostgreSQL sessions display and interpret zoned timestamps.
-- The Flyway user must own the database (or be a superuser).
DO $$
DECLARE
    database_name text := current_database();
BEGIN
    EXECUTE format(
        'ALTER DATABASE %I SET timezone TO %L',
        database_name,
        'Asia/Ho_Chi_Minh'
    );
END
$$;

-- Apply the same timezone to the current Flyway session immediately. The
-- database-level default above applies to subsequent sessions.
SET TIME ZONE 'Asia/Ho_Chi_Minh';
