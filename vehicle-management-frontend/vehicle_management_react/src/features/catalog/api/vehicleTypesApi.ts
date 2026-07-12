import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type VehicleTypeFilter = {
  isActive?: boolean;
};

export type CreateVehicleTypeRequest = {
  code: string;
  description?: string | null;
  isActive?: boolean | null;
  name: string;
};

export type UpdateVehicleTypeRequest = CreateVehicleTypeRequest;

export type VehicleTypeApiResponse = {
  code: string;
  createdAt: string | null;
  createdBy: string | null;
  description: string | null;
  isActive: boolean | null;
  name: string;
  updatedAt: string | null;
  updatedBy: string | null;
  vehicleTypeId: string;
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

export function getVehicleTypes(filter: VehicleTypeFilter = {}) {
  return apiClient<ApiResponse<VehicleTypeApiResponse[]>>(
    `${apiEndpoints.catalog.vehicleTypes}${buildQuery(filter)}`,
  );
}

export function createVehicleType(payload: CreateVehicleTypeRequest) {
  return apiClient<ApiResponse<VehicleTypeApiResponse>>(apiEndpoints.catalog.vehicleTypes, {
    method: "POST",
    body: payload,
  });
}

export function updateVehicleType(vehicleTypeId: string, payload: UpdateVehicleTypeRequest) {
  return apiClient<ApiResponse<VehicleTypeApiResponse>>(`${apiEndpoints.catalog.vehicleTypes}/${vehicleTypeId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivateVehicleType(vehicleTypeId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.catalog.vehicleTypes}/${vehicleTypeId}`, {
    method: "DELETE",
  });
}

export function activateVehicleType(vehicleTypeId: string) {
  return apiClient<ApiResponse<VehicleTypeApiResponse>>(`${apiEndpoints.catalog.vehicleTypes}/${vehicleTypeId}/activate`, {
    method: "PATCH",
  });
}
