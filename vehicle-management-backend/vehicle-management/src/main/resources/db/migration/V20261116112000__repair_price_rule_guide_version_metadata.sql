DROP TABLE IF EXISTS pg_temp.price_rule_guide_version_repair;
CREATE TEMP TABLE price_rule_guide_version_repair (
    guide_code varchar(120) PRIMARY KEY,
    guide_version_id uuid NOT NULL
) ON COMMIT DROP;

INSERT INTO price_rule_guide_version_repair VALUES
('PRICE_RULE_SETUP', md5('price-rule-guide:PRICE_RULE_SETUP:2')::uuid),
('WORKFLOW_RULE_DRAFT', md5('price-rule-guide:WORKFLOW_RULE_DRAFT:2')::uuid);

-- Guide step order is zero-based throughout the user-guide runtime. Move the
-- rows out of the unique-index range first, then normalize 1..N to 0..N-1.
-- Only versions still using 1..N need repair; reruns must preserve 0..N-1.
DROP TABLE IF EXISTS pg_temp.price_rule_guide_step_order_repair;
CREATE TEMP TABLE price_rule_guide_step_order_repair ON COMMIT DROP AS
SELECT repair.guide_version_id
FROM price_rule_guide_version_repair repair
JOIN notification.user_guide_steps step
  ON step.guide_version_id = repair.guide_version_id
GROUP BY repair.guide_version_id
HAVING min(step.display_order) = 1
   AND max(step.display_order) = count(*);

UPDATE notification.user_guide_steps step
SET display_order = step.display_order + 100
FROM price_rule_guide_step_order_repair repair
WHERE step.guide_version_id = repair.guide_version_id;

UPDATE notification.user_guide_steps step
SET display_order = step.display_order - 101
FROM price_rule_guide_step_order_repair repair
WHERE step.guide_version_id = repair.guide_version_id;

-- V20261116111000 selected the newest version after inserting version 2, so it
-- attempted to copy audiences from the empty target version. Copy explicitly
-- from version 1 while preserving the existing role/permission constraints.
INSERT INTO notification.user_guide_audiences (
    audience_id, guide_version_id, target_type, role_id, permission_id
)
SELECT gen_random_uuid(), repair.guide_version_id, audience.target_type,
       audience.role_id, audience.permission_id
FROM price_rule_guide_version_repair repair
JOIN notification.user_guides guide ON guide.code = repair.guide_code
JOIN notification.user_guide_versions previous
  ON previous.guide_id = guide.guide_id AND previous.version_number = 1
JOIN notification.user_guide_audiences audience
  ON audience.guide_version_id = previous.guide_version_id
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM price_rule_guide_version_repair repair
        LEFT JOIN notification.user_guide_steps step
          ON step.guide_version_id = repair.guide_version_id
        LEFT JOIN notification.user_guide_audiences audience
          ON audience.guide_version_id = repair.guide_version_id
        GROUP BY repair.guide_version_id
        HAVING min(step.display_order) <> 0
            OR max(step.display_order) <> count(DISTINCT step.step_id) - 1
            OR count(DISTINCT audience.audience_id) = 0
    ) THEN
        RAISE EXCEPTION 'Price-rule guide version repair is incomplete';
    END IF;
END $$;
