ALTER TABLE operations.shift_assignments
    DROP CONSTRAINT IF EXISTS ck_shift_assignments_status;

ALTER TABLE operations.shift_assignments
    ADD CONSTRAINT ck_shift_assignments_status
        CHECK (status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'REMOVED'));

UPDATE operations.shift_assignments assignment_item
SET status = 'DRAFT'
FROM operations.shifts shift_item
WHERE assignment_item.shift_id = shift_item.shift_id
  AND shift_item.status = 'DRAFT'
  AND assignment_item.status = 'ACTIVE';

UPDATE operations.shift_assignments assignment_item
SET status = 'SCHEDULED'
FROM operations.shifts shift_item
WHERE assignment_item.shift_id = shift_item.shift_id
  AND shift_item.status = 'SCHEDULED'
  AND assignment_item.status = 'ACTIVE';