import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [page, api, endpoints] = await Promise.all([
  readFile(new URL("../src/features/employees/pages/ShiftSchedulePage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/employees/api/shiftsApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/core/api/apiEndpoints.ts", import.meta.url), "utf8"),
]);

// API contract must preserve organization + parking-lot scope in the URL.
assert.match(endpoints, /parkingLotAssignments:\s*"\/iam\/parking-lot-assignments"/);
assert.match(api, /assignEmployeeToParkingLot[\s\S]*?organizationId[\s\S]*?parkingLotId[\s\S]*?parkingLotAssignments}\/organizations\/\$\{organizationId}\/parking-lots\/\$\{parkingLotId}\/employees/);
assert.match(api, /getEmployeeParkingLotAssignments[\s\S]*?organizations\/\$\{organizationId}\/employees/);
assert.match(api, /transferEmployeeToParkingLot[\s\S]*?organizations\/\$\{organizationId}\/employees\/transfer/);
assert.match(api, /assignmentType:\s*EmployeeParkingLotAssignmentTypeApi/);

// The Partner action is permission-first and never authorized from a role label.
assert.match(page, /hasAnyPermission\(user, \["EMPLOYEE_UPDATE_ALL"\]\)[\s\S]*?hasAnyPermission\(user, \["PARKING_SCOPE_PARTNER"\]\)/);
assert.doesNotMatch(page, /user\??\.role\s*===\s*"PARTNER_ADMIN"/);

// Only approved operational candidates are offered for immediate assignment.
assert.match(page, /employee\.roleCode === "EMPLOYEE"[\s\S]*?employee\.status === "ACTIVE"[\s\S]*?employee\.accountStatus === "ACTIVE"/);
assert.match(page, /parkingLot\.status === "ACTIVE"/);

// The selected lot supplies both tenant and resource identifiers; refresh after mutation.
assert.match(page, /assignEmployeeToParkingLot\([\s\S]*?parkingLot\.organizationId,[\s\S]*?parkingLot\.parkingLotId,[\s\S]*?employeeId/);
assert.match(page, /await onAssigned\(\);[\s\S]*?onClose\(\)/);
assert.match(page, /Phân công nhân viên vào bãi/);

// Partner has a complete management lifecycle: list, history, and scoped transfer.
assert.match(page, /Phân công bãi xe/);
assert.match(page, /Danh sách phân công bãi xe/);
assert.match(page, /Chuyển công tác sang bãi khác/);
assert.match(page, /transferEmployeeToParkingLot\(assignment\.organizationId,[\s\S]*?sourceParkingLotId:[\s\S]*?targetParkingLotId/);
assert.match(page, /hasAnyPermission\(user, \["ORGANIZATION_MEMBERSHIP_MANAGE_ALL"\]\)/);

console.log("Employee parking-lot assignment permission, scope, API, and UI checks passed.");
