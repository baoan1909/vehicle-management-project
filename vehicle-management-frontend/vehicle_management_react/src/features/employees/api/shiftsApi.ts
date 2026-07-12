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
