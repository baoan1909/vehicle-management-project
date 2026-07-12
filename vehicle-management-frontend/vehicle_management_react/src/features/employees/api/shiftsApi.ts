import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type ShiftTypeApi = "MORNING" | "AFTERNOON" | "NIGHT";
export type ShiftStatusApi = "DRAFT" | "SCHEDULED" | "OPEN" | "CLOSED" | "CANCELLED";
export type ShiftAssignmentStatusApi = "DRAFT" | "SCHEDULED" | "ACTIVE" | "REMOVED";
export type ShiftTemplateStatusApi = "ACTIVE" | "INACTIVE";
export type RosterRuleStatusApi = "ACTIVE" | "INACTIVE";
export type AssignmentModeApi = "FIXED" | "RELIEF";
export type WeekdayApi = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";

export type ShiftFilter = {
  employeeId?: string;
  fromDate?: string;
  keyword?: string;
  parkingLotId?: string;
  shiftType?: ShiftTypeApi;
  status?: ShiftStatusApi;
  toDate?: string;
};

export type ShiftApiResponse = {
  approvedAt?: string | null;
  approvedBy?: string | null;
  cancelledAt?: string | null;
  cancelledBy?: string | null;
  cancellationReason?: string | null;
  closedAt?: string | null;
  closedBy?: string | null;
  closingCash?: number | string | null;
  createdAt?: string | null;
  createdBy?: string | null;
  endTime?: string | null;
  note?: string | null;
  openedAt?: string | null;
  openedBy?: string | null;
  openingCash?: number | string | null;
  parkingLotId: string;
  shiftCode: string;
  shiftDate: string;
  shiftId: string;
  shiftTemplateId?: string | null;
  shiftType: ShiftTypeApi;
  startTime?: string | null;
  status: ShiftStatusApi;
  updatedAt?: string | null;
  updatedBy?: string | null;
};

export type ShiftAssignmentFilter = {
  employeeId?: string;
  fromDate?: string;
  gateId?: string;
  parkingLotId?: string;
  shiftId?: string;
  shiftType?: ShiftTypeApi;
  status?: ShiftAssignmentStatusApi;
  toDate?: string;
};

export type ShiftAssignmentApiResponse = {
  createdAt?: string | null;
  createdBy?: string | null;
  employeeId: string;
  gateId?: string | null;
  shiftAssignmentId: string;
  shiftId: string;
  status: ShiftAssignmentStatusApi;
  updatedAt?: string | null;
  updatedBy?: string | null;
};

export type ShiftTemplateFilter = {
  keyword?: string;
  parkingLotId?: string;
  shiftType?: ShiftTypeApi;
  status?: ShiftTemplateStatusApi;
};

export type ShiftTemplateApiResponse = {
  createdAt?: string | null;
  createdBy?: string | null;
  endLocalTime: string;
  name: string;
  parkingLotId: string;
  shiftTemplateId: string;
  shiftType: ShiftTypeApi;
  startLocalTime: string;
  status: ShiftTemplateStatusApi;
  updatedAt?: string | null;
  updatedBy?: string | null;
};

export type UpsertShiftTemplateRequest = {
  endLocalTime: string;
  name: string;
  parkingLotId?: string;
  shiftType?: ShiftTypeApi;
  startLocalTime: string;
};

export type EmployeeRosterRuleFilter = {
  assignmentMode?: AssignmentModeApi;
  effectiveDate?: string;
  employeeId?: string;
  parkingLotId?: string;
  preferredGateId?: string;
  preferredShiftType?: ShiftTypeApi;
  status?: RosterRuleStatusApi;
  weeklyDayOff?: WeekdayApi;
};

export type EmployeeRosterRuleApiResponse = {
  assignmentMode: AssignmentModeApi;
  createdAt?: string | null;
  createdBy?: string | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  employeeId: string;
  parkingLotId: string;
  preferredGateId?: string | null;
  preferredShiftType: ShiftTypeApi;
  rosterRuleId: string;
  status: RosterRuleStatusApi;
  updatedAt?: string | null;
  updatedBy?: string | null;
  weeklyDayOff: WeekdayApi;
};

export type UpsertEmployeeRosterRuleRequest = {
  assignmentMode: AssignmentModeApi;
  effectiveFrom: string;
  effectiveTo?: string | null;
  employeeId: string;
  parkingLotId: string;
  preferredGateId?: string | null;
  preferredShiftType: ShiftTypeApi;
  weeklyDayOff: WeekdayApi;
};

export type UpsertShiftAssignmentRequest = {
  employeeId: string;
  gateId?: string | null;
};

export type ReplaceShiftAssignmentRequest = {
  reason?: string | null;
  replacementEmployeeId: string;
};

export type SwapShiftAssignmentRequest = {
  firstAssignmentId: string;
  reason?: string | null;
  secondAssignmentId: string;
};

export type ParkingLotApiResponse = {
  address?: string | null;
  code: string;
  name: string;
  parkingLotId: string;
  status?: string | null;
  totalCapacity?: number | null;
};

export type GateApiResponse = {
  code: string;
  gateId: string;
  name: string;
  status?: string | null;
  zoneId?: string | null;
};

function buildQuery(filter: Record<string, string | number | boolean | null | undefined>) {
  const params = new URLSearchParams();

  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });

  const query = params.toString();
  return query ? `?${query}` : "";
}

export function getShifts(filter: ShiftFilter = {}) {
  return apiClient<ApiResponse<ShiftApiResponse[]>>(
    `${apiEndpoints.operations.shifts}${buildQuery(filter)}`,
  );
}

export function getShiftAssignments(filter: ShiftAssignmentFilter = {}) {
  return apiClient<ApiResponse<ShiftAssignmentApiResponse[]>>(
    `${apiEndpoints.operations.shiftAssignments}${buildQuery(filter)}`,
  );
}

export function createShiftAssignment(shiftId: string, payload: UpsertShiftAssignmentRequest) {
  return apiClient<ApiResponse<ShiftAssignmentApiResponse>>(`${apiEndpoints.operations.shifts}/${shiftId}/assignments`, {
    body: payload,
    method: "POST",
  });
}

export function updateShiftAssignment(assignmentId: string, payload: UpsertShiftAssignmentRequest) {
  return apiClient<ApiResponse<ShiftAssignmentApiResponse>>(`${apiEndpoints.operations.shiftAssignments}/${assignmentId}`, {
    body: payload,
    method: "PUT",
  });
}

export function replaceShiftAssignment(assignmentId: string, payload: ReplaceShiftAssignmentRequest) {
  return apiClient<ApiResponse<ShiftAssignmentApiResponse>>(`${apiEndpoints.operations.shiftAssignments}/${assignmentId}/replace`, {
    body: payload,
    method: "PATCH",
  });
}

export function swapShiftAssignments(payload: SwapShiftAssignmentRequest) {
  return apiClient<ApiResponse<ShiftAssignmentApiResponse[]>>(`${apiEndpoints.operations.shiftAssignments}/swap`, {
    body: payload,
    method: "POST",
  });
}

export function deleteShiftAssignment(assignmentId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.operations.shiftAssignments}/${assignmentId}`, {
    method: "DELETE",
  });
}

export function getShiftTemplates(filter: ShiftTemplateFilter = {}) {
  return apiClient<ApiResponse<ShiftTemplateApiResponse[]>>(
    `${apiEndpoints.operations.shiftTemplates}${buildQuery(filter)}`,
  );
}

export function createShiftTemplate(payload: UpsertShiftTemplateRequest & { parkingLotId: string; shiftType: ShiftTypeApi }) {
  return apiClient<ApiResponse<ShiftTemplateApiResponse>>(apiEndpoints.operations.shiftTemplates, {
    body: payload,
    method: "POST",
  });
}

export function updateShiftTemplate(templateId: string, payload: UpsertShiftTemplateRequest) {
  return apiClient<ApiResponse<ShiftTemplateApiResponse>>(`${apiEndpoints.operations.shiftTemplates}/${templateId}`, {
    body: payload,
    method: "PUT",
  });
}

export function activateShiftTemplate(templateId: string) {
  return apiClient<ApiResponse<ShiftTemplateApiResponse>>(`${apiEndpoints.operations.shiftTemplates}/${templateId}/activate`, {
    method: "PATCH",
  });
}

export function deleteShiftTemplate(templateId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.operations.shiftTemplates}/${templateId}`, {
    method: "DELETE",
  });
}

export function getEmployeeRosterRules(filter: EmployeeRosterRuleFilter = {}) {
  return apiClient<ApiResponse<EmployeeRosterRuleApiResponse[]>>(
    `${apiEndpoints.operations.employeeRosterRules}${buildQuery(filter)}`,
  );
}

export function createEmployeeRosterRule(payload: UpsertEmployeeRosterRuleRequest) {
  return apiClient<ApiResponse<EmployeeRosterRuleApiResponse>>(apiEndpoints.operations.employeeRosterRules, {
    body: payload,
    method: "POST",
  });
}

export function updateEmployeeRosterRule(ruleId: string, payload: UpsertEmployeeRosterRuleRequest) {
  return apiClient<ApiResponse<EmployeeRosterRuleApiResponse>>(`${apiEndpoints.operations.employeeRosterRules}/${ruleId}`, {
    body: payload,
    method: "PUT",
  });
}

export function activateEmployeeRosterRule(ruleId: string) {
  return apiClient<ApiResponse<EmployeeRosterRuleApiResponse>>(`${apiEndpoints.operations.employeeRosterRules}/${ruleId}/activate`, {
    method: "PATCH",
  });
}

export function deleteEmployeeRosterRule(ruleId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.operations.employeeRosterRules}/${ruleId}`, {
    method: "DELETE",
  });
}

export function getParkingLots() {
  return apiClient<ApiResponse<ParkingLotApiResponse[]>>(apiEndpoints.parking.parkingLots);
}

export function getGates() {
  return apiClient<ApiResponse<GateApiResponse[]>>(apiEndpoints.parking.gates);
}

export function generateWorkScheduleWeek(parkingLotId: string, weekStartDate: string) {
  return apiClient<ApiResponse<ShiftApiResponse[]>>(apiEndpoints.operations.generateWorkScheduleWeek, {
    body: { parkingLotId, weekStartDate },
    method: "POST",
  });
}

export function approveWorkScheduleWeek(parkingLotId: string, weekStartDate: string) {
  return apiClient<ApiResponse<ShiftApiResponse[]>>(apiEndpoints.operations.approveWorkScheduleWeek, {
    body: { parkingLotId, weekStartDate },
    method: "PATCH",
  });
}
