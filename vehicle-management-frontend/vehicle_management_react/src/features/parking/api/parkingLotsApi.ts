import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type ParkingLotStatusApi = "ACTIVE" | "MAINTENANCE" | "CLOSED";

export type ParkingLotApiResponse = {
  address: string | null;
  code: string;
  createdAt?: string | null;
  createdBy?: string | null;
  name: string;
  parkingLotId: string;
  status: ParkingLotStatusApi;
  totalCapacity: number | null;
  updatedAt?: string | null;
  updatedBy?: string | null;
};

export type ParkingLotFilter = {
  keyword?: string;
  status?: ParkingLotStatusApi;
};

export type UpsertParkingLotRequest = {
  address: string;
  code: string;
  name: string;
  totalCapacity: number;
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

export function getParkingLots(filter: ParkingLotFilter = {}) {
  return apiClient<ApiResponse<ParkingLotApiResponse[]>>(
    `${apiEndpoints.parking.parkingLots}${buildQuery(filter)}`,
  );
}

export function createParkingLot(payload: UpsertParkingLotRequest) {
  return apiClient<ApiResponse<ParkingLotApiResponse>>(apiEndpoints.parking.parkingLots, {
    body: payload,
    method: "POST",
  });
}

export function updateParkingLot(parkingLotId: string, payload: UpsertParkingLotRequest) {
  return apiClient<ApiResponse<ParkingLotApiResponse>>(`${apiEndpoints.parking.parkingLots}/${parkingLotId}`, {
    body: payload,
    method: "PUT",
  });
}

export function activateParkingLot(parkingLotId: string) {
  return apiClient<ApiResponse<ParkingLotApiResponse>>(`${apiEndpoints.parking.parkingLots}/${parkingLotId}/activate`, {
    method: "PATCH",
  });
}

export function markParkingLotMaintenance(parkingLotId: string) {
  return apiClient<ApiResponse<ParkingLotApiResponse>>(`${apiEndpoints.parking.parkingLots}/${parkingLotId}/maintenance`, {
    method: "PATCH",
  });
}

export function closeParkingLot(parkingLotId: string) {
  return apiClient<ApiResponse<ParkingLotApiResponse>>(`${apiEndpoints.parking.parkingLots}/${parkingLotId}/close`, {
    method: "PATCH",
  });
}

export function deleteParkingLot(parkingLotId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.parking.parkingLots}/${parkingLotId}`, {
    method: "DELETE",
  });
}
