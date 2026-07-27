import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type ZoneStatusApi = "ACTIVE" | "MAINTENANCE" | "CLOSED";
export type GateStatusApi = "ACTIVE" | "MAINTENANCE" | "CLOSED";
export type LaneStatusApi = "ACTIVE" | "MAINTENANCE" | "CLOSED";
export type LaneDirectionApi = "IN" | "OUT";

export type ZoneApiResponse = {
  capacity: number | null;
  code: string;
  createdAt?: string | null;
  createdBy?: string | null;
  name: string;
  parkingLotId: string;
  status: ZoneStatusApi;
  updatedAt?: string | null;
  updatedBy?: string | null;
  vehicleTypeId: string | null;
  zoneId: string;
};

export type GateApiResponse = {
  code: string;
  createdAt?: string | null;
  createdBy?: string | null;
  gateId: string;
  name: string;
  status: GateStatusApi;
  updatedAt?: string | null;
  updatedBy?: string | null;
  zoneId: string;
};

export type LaneApiResponse = {
  code: string;
  createdAt?: string | null;
  createdBy?: string | null;
  direction: LaneDirectionApi;
  gateId: string;
  laneId: string;
  name: string;
  status: LaneStatusApi;
  updatedAt?: string | null;
  updatedBy?: string | null;
};

export type ZoneFilter = {
  keyword?: string;
  parkingLotId?: string;
  status?: ZoneStatusApi;
  vehicleTypeId?: string;
};

export type GateFilter = {
  keyword?: string;
  status?: GateStatusApi;
  zoneId?: string;
};

export type LaneFilter = {
  direction?: LaneDirectionApi;
  gateId?: string;
  keyword?: string;
  status?: LaneStatusApi;
};

export type UpdateZoneRequest = {
  capacity: number;
  code: string;
  name: string;
  vehicleTypeId: string | null;
};

export type CreateZoneRequest = UpdateZoneRequest & {
  parkingLotId: string;
};

export type UpdateGateRequest = {
  code: string;
  name: string;
};

export type CreateGateRequest = UpdateGateRequest & {
  zoneId: string;
};

export type UpdateLaneRequest = {
  code: string;
  direction: LaneDirectionApi;
  name: string;
};

export type CreateLaneRequest = UpdateLaneRequest & {
  gateId: string;
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

export function getZones(filter: ZoneFilter = {}) {
  return apiClient<ApiResponse<ZoneApiResponse[]>>(
    `${apiEndpoints.parking.zones}${buildQuery(filter)}`,
  );
}

export function getGates(filter: GateFilter = {}) {
  return apiClient<ApiResponse<GateApiResponse[]>>(
    `${apiEndpoints.parking.gates}${buildQuery(filter)}`,
  );
}

export function getLanes(filter: LaneFilter = {}) {
  return apiClient<ApiResponse<LaneApiResponse[]>>(
    `${apiEndpoints.parking.lanes}${buildQuery(filter)}`,
  );
}

export function createZone(payload: CreateZoneRequest) {
  return apiClient<ApiResponse<ZoneApiResponse>>(apiEndpoints.parking.zones, {
    body: payload,
    method: "POST",
  });
}

export function updateZone(zoneId: string, payload: UpdateZoneRequest) {
  return apiClient<ApiResponse<ZoneApiResponse>>(`${apiEndpoints.parking.zones}/${zoneId}`, {
    body: payload,
    method: "PUT",
  });
}

export function activateZone(zoneId: string) {
  return apiClient<ApiResponse<ZoneApiResponse>>(`${apiEndpoints.parking.zones}/${zoneId}/activate`, {
    method: "PATCH",
  });
}

export function markZoneMaintenance(zoneId: string) {
  return apiClient<ApiResponse<ZoneApiResponse>>(`${apiEndpoints.parking.zones}/${zoneId}/maintenance`, {
    method: "PATCH",
  });
}

export function closeZone(zoneId: string) {
  return apiClient<ApiResponse<ZoneApiResponse>>(`${apiEndpoints.parking.zones}/${zoneId}/close`, {
    method: "PATCH",
  });
}

export function createGate(payload: CreateGateRequest) {
  return apiClient<ApiResponse<GateApiResponse>>(apiEndpoints.parking.gates, {
    body: payload,
    method: "POST",
  });
}

export function updateGate(gateId: string, payload: UpdateGateRequest) {
  return apiClient<ApiResponse<GateApiResponse>>(`${apiEndpoints.parking.gates}/${gateId}`, {
    body: payload,
    method: "PUT",
  });
}

export function activateGate(gateId: string) {
  return apiClient<ApiResponse<GateApiResponse>>(`${apiEndpoints.parking.gates}/${gateId}/activate`, {
    method: "PATCH",
  });
}

export function markGateMaintenance(gateId: string) {
  return apiClient<ApiResponse<GateApiResponse>>(`${apiEndpoints.parking.gates}/${gateId}/maintenance`, {
    method: "PATCH",
  });
}

export function closeGate(gateId: string) {
  return apiClient<ApiResponse<GateApiResponse>>(`${apiEndpoints.parking.gates}/${gateId}/close`, {
    method: "PATCH",
  });
}

export function createLane(payload: CreateLaneRequest) {
  return apiClient<ApiResponse<LaneApiResponse>>(apiEndpoints.parking.lanes, {
    body: payload,
    method: "POST",
  });
}

export function updateLane(laneId: string, payload: UpdateLaneRequest) {
  return apiClient<ApiResponse<LaneApiResponse>>(`${apiEndpoints.parking.lanes}/${laneId}`, {
    body: payload,
    method: "PUT",
  });
}

export function activateLane(laneId: string) {
  return apiClient<ApiResponse<LaneApiResponse>>(`${apiEndpoints.parking.lanes}/${laneId}/activate`, {
    method: "PATCH",
  });
}

export function markLaneMaintenance(laneId: string) {
  return apiClient<ApiResponse<LaneApiResponse>>(`${apiEndpoints.parking.lanes}/${laneId}/maintenance`, {
    method: "PATCH",
  });
}

export function closeLane(laneId: string) {
  return apiClient<ApiResponse<LaneApiResponse>>(`${apiEndpoints.parking.lanes}/${laneId}/close`, {
    method: "PATCH",
  });
}
