-- Usage: psql "$DATABASE_URL" -v organization_id='UUID' -f partner-catalog-draft-dry-run.sql
-- Requires V20261115100000. SELECT only: no records are changed or auto-classified as safe.
\set ON_ERROR_STOP on
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

SELECT organization.organization_id, organization.code, organization.status,
       organization.catalog_seeded_at, membership.account_id, membership.status AS membership_status
FROM iam.organizations organization
LEFT JOIN iam.organization_memberships membership USING (organization_id)
WHERE organization.organization_id = :'organization_id'::uuid;

-- Manifest: preserve exact IDs, snapshots and source lineage for manual investigation.
WITH items AS (
    SELECT 'vehicle_types' AS catalog, vehicle_type_id AS id, organization_id,
           lifecycle_status AS state, source_catalog_id, to_jsonb(item) AS snapshot
    FROM catalog.vehicle_types item
    UNION ALL
    SELECT 'ticket_types', ticket_type_id, organization_id, status, source_catalog_id, to_jsonb(item)
    FROM catalog.ticket_types item
    UNION ALL
    SELECT 'price_plans', price_plan_id, organization_id, lifecycle_status, source_catalog_id, to_jsonb(item)
    FROM catalog.price_plans item
    UNION ALL
    SELECT 'price_rules', price_rule_id, organization_id, lifecycle_status, source_catalog_id, to_jsonb(item)
    FROM catalog.price_rules item
)
SELECT catalog, id, state AS before_state, state AS retained_state,
       NULL::text AS proposed_state, source_catalog_id,
       CASE WHEN source_catalog_id IS NULL THEN 'UNKNOWN_ORIGIN' ELSE 'RECORDED_SOURCE_ID' END AS lineage,
       false AS automatic_remediation_allowed, snapshot
FROM items WHERE organization_id = :'organization_id'::uuid
ORDER BY catalog, id;

-- Conservative usage evidence includes all history, not only current obligations.
SELECT 'subscriptions' AS evidence, item.subscription_id AS id, item.status,
       item.ticket_type_id, item.price_rule_id, to_jsonb(item) AS snapshot
FROM access_control.subscriptions item
JOIN parking.parking_lots lot USING (parking_lot_id)
WHERE lot.organization_id = :'organization_id'::uuid;

SELECT 'renewals' AS evidence, to_jsonb(renewal) AS snapshot
FROM access_control.subscription_renewals renewal
JOIN catalog.price_rules rule USING (price_rule_id)
WHERE rule.organization_id = :'organization_id'::uuid;

SELECT 'sessions' AS evidence, item.parking_session_id AS id, item.status,
       item.vehicle_type_id, item.check_out_time, to_jsonb(item) AS snapshot
FROM parking.parking_sessions item
JOIN parking.parking_lots lot USING (parking_lot_id)
WHERE lot.organization_id = :'organization_id'::uuid;

SELECT 'reservations' AS evidence, to_jsonb(item) AS snapshot
FROM parking.customer_reservations item
JOIN parking.parking_lots lot USING (parking_lot_id)
WHERE lot.organization_id = :'organization_id'::uuid;

SELECT 'invoices' AS evidence, to_jsonb(item) AS snapshot
FROM billing.invoices item
JOIN parking.parking_lots lot USING (parking_lot_id)
WHERE lot.organization_id = :'organization_id'::uuid;

SELECT 'audit' AS evidence, log.target_table, log.target_id, log.actor_account_id,
       log.action, log.created_at, log.old_data, log.new_data
FROM audit.audit_logs log
WHERE log.target_schema = 'catalog'
  AND (log.new_data ->> 'organization_id' = :'organization_id'
       OR log.old_data ->> 'organization_id' = :'organization_id')
ORDER BY log.created_at, log.audit_log_id;

ROLLBACK;
