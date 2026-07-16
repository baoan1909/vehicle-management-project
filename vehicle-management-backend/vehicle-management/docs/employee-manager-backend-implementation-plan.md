# Employee Manager Backend Implementation Plan

## Current Phase

The employee manager screen keeps the original three-column mockup:

- Left: employee list, filters, selected employee.
- Middle: employee profile and recent shifts.
- Right: account/role summary, pending work, recent activity.

The backend already supports the core employee profile actions through:

- `GET /api/people/employees`
- `GET /api/people/employees/{employeeId}`
- `PUT /api/people/employees/{employeeId}`
- `PATCH /api/people/employees/{employeeId}/activate`
- `PATCH /api/people/employees/{employeeId}/inactivate`
- `PATCH /api/people/employees/{employeeId}/suspend`

This phase adds two read-optimized APIs so the mockup can be populated without frontend mock data.

## Implemented API 1: Recent Shifts By Employee

### Endpoint

`GET /api/people/employees/{employeeId}/recent-shifts?limit=3`

### Purpose

Return the latest shift assignments for the selected employee in a compact format for the employee profile screen.

### Response Shape

```json
{
  "success": true,
  "message": "Fetched recent employee shifts successfully",
  "data": [
    {
      "shiftId": "uuid",
      "assignmentId": "uuid",
      "shiftDate": "2026-07-13",
      "shiftType": "MORNING",
      "timeRange": "07:00 - 11:30",
      "locationName": "Cong A",
      "roleInShift": "OPERATOR",
      "status": "SCHEDULED"
    }
  ],
  "timestamp": "2026-07-13T10:00:00Z"
}
```

### Implementation Notes

- Query from shift assignments directly instead of overloading the full shift calendar API.
- Sort by shift date/time descending.
- Exclude removed assignments.
- Reuse employee read permission and access guard.
- `roleInShift` currently returns `OPERATOR` because the old `role_in_shift` column was removed from the shift structure migration.

## Implemented API 2: Employee Activity Timeline

### Endpoint

`GET /api/people/employees/{employeeId}/activity-timeline?limit=5`

### Purpose

Return audit-style events that explain what recently happened to the employee profile/account.

### Response Shape

```json
{
  "success": true,
  "message": "Fetched employee activity timeline successfully",
  "data": [
    {
      "eventId": "uuid",
      "eventTime": "2026-07-13T09:15:00Z",
      "eventType": "ACCOUNT_STATUS_CHANGED",
      "title": "Cap nhat trang thai tai khoan",
      "description": "Tai khoan chuyen sang trang thai ACTIVE.",
      "actorAccountId": "uuid",
      "actorName": "Nguyen Van Manager"
    }
  ],
  "timestamp": "2026-07-13T10:00:00Z"
}
```

### Event Sources

- `audit.audit_logs` for employee profile changes.
- `iam.account_status_history` for linked account lock/disable/activate events.
- `operations.approval_requests` for internal employee onboarding approval.

## Frontend Integration Plan

1. Add `recentShifts` and `activityTimeline` functions to `features/employees/api/employeesApi.ts`.
2. Load both datasets when `selectedEmployee.id` changes.
3. Replace current empty states in recent shifts and recent activity blocks.
4. Keep empty states if API returns an empty array.

## Out Of Scope For Current Phase

- Creating a new employee directly from the employee screen.
- Replacing the existing account provisioning flow.
- Full shift calendar redesign.
