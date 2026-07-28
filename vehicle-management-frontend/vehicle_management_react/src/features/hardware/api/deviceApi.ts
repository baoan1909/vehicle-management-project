import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type DeviceTypeApi = "CAMERA" | "KIOSK" | "CARD_READER" | "BARRIER";
export type DeviceStatusApi = "ACTIVE" | "OFFLINE" | "MAINTENANCE" | "RETIRED";

export type DeviceApiResponse = {
  config: Record<string, unknown> | null;
  createdAt: string | null;
  createdBy: string | null;
  deviceCode: string;
  deviceId: string;
  deviceType: DeviceTypeApi;
  ipAddress: string | null;
  laneId: string | null;
  name: string;
  parkingLotId: string;
  status: DeviceStatusApi;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type DeviceFilter = {
  deviceType?: DeviceTypeApi;
  keyword?: string;
  laneId?: string;
  parkingLotId?: string;
  status?: DeviceStatusApi;
};

export type SaveDeviceRequest = {
  config: Record<string, unknown> | null;
  deviceCode: string;
  deviceType: DeviceTypeApi;
  ipAddress: string | null;
  laneId: string | null;
  name: string;
  parkingLotId: string;
};

function buildQuery(filter: DeviceFilter) {
  const params = new URLSearchParams();

  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, value);
    }
  });

  const query = params.toString();
  return query ? `?${query}` : "";
}

export function getDevices(filter: DeviceFilter = {}) {
  return apiClient<ApiResponse<DeviceApiResponse[]>>(
    `${apiEndpoints.hardware.devices}${buildQuery(filter)}`,
  );
}

export function getDeviceById(deviceId: string) {
  return apiClient<ApiResponse<DeviceApiResponse>>(
    `${apiEndpoints.hardware.devices}/${deviceId}`,
  );
}

export function createDevice(payload: SaveDeviceRequest) {
  return apiClient<ApiResponse<DeviceApiResponse>>(apiEndpoints.hardware.devices, {
    body: payload,
    method: "POST",
  });
}

export function updateDevice(deviceId: string, payload: SaveDeviceRequest) {
  return apiClient<ApiResponse<DeviceApiResponse>>(
    `${apiEndpoints.hardware.devices}/${deviceId}`,
    {
      body: payload,
      method: "PUT",
    },
  );
}

export function activateDevice(deviceId: string) {
  return apiClient<ApiResponse<DeviceApiResponse>>(
    `${apiEndpoints.hardware.devices}/${deviceId}/activate`,
    { method: "PATCH" },
  );
}

export function markDeviceOffline(deviceId: string) {
  return apiClient<ApiResponse<DeviceApiResponse>>(
    `${apiEndpoints.hardware.devices}/${deviceId}/offline`,
    { method: "PATCH" },
  );
}

export function markDeviceMaintenance(deviceId: string) {
  return apiClient<ApiResponse<DeviceApiResponse>>(
    `${apiEndpoints.hardware.devices}/${deviceId}/maintenance`,
    { method: "PATCH" },
  );
}

export function retireDevice(deviceId: string) {
  return apiClient<ApiResponse<void>>(
    `${apiEndpoints.hardware.devices}/${deviceId}`,
    { method: "DELETE" },
  );
}
