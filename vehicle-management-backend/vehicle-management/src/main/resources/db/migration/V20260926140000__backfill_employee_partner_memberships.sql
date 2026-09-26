-- Existing Employee accounts may predate Partner memberships. Infer ownership
-- only when creator and historical shift evidence agree on exactly one Partner.
-- Ambiguous/untraceable accounts remain unassigned and cannot be scheduled
-- until explicitly reviewed; never attach them to an arbitrary local UUID.
WITH inferred_organizations AS (
    SELECT account.account_id, creator_membership.organization_id
    FROM iam.accounts account
    JOIN iam.roles role ON role.role_id = account.role_id AND role.code = 'EMPLOYEE'
    JOIN iam.organization_memberships creator_membership
      ON creator_membership.account_id = account.created_by
     AND creator_membership.status = 'ACTIVE'
    UNION
    SELECT account.account_id, lot.organization_id
    FROM people.employees employee
    JOIN iam.accounts account ON account.user_profile_id = employee.user_profile_id
    JOIN iam.roles role ON role.role_id = account.role_id AND role.code = 'EMPLOYEE'
    JOIN operations.shift_assignments assignment ON assignment.employee_id = employee.employee_id
    JOIN operations.shifts shift ON shift.shift_id = assignment.shift_id
    JOIN parking.parking_lots lot ON lot.parking_lot_id = shift.parking_lot_id
), unambiguous AS (
    SELECT account_id, MIN(organization_id::text)::uuid AS organization_id
    FROM inferred_organizations
    GROUP BY account_id
    HAVING COUNT(DISTINCT organization_id) = 1
)
INSERT INTO iam.organization_memberships (
    organization_membership_id, organization_id, account_id, status, created_at
)
SELECT gen_random_uuid(), candidate.organization_id, candidate.account_id, 'ACTIVE', now()
FROM unambiguous candidate
WHERE NOT EXISTS (
    SELECT 1 FROM iam.organization_memberships existing
    WHERE existing.account_id = candidate.account_id
)
ON CONFLICT (organization_id, account_id) DO NOTHING;
