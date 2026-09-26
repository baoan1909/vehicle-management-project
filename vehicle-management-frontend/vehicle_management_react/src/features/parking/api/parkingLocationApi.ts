import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type ParkingLocationResponse = {
  displayName: string;
  latitude: number;
  longitude: number;
};

export function searchParkingLocations(query: string) {
  return apiClient<ApiResponse<ParkingLocationResponse[]>>(
    `${apiEndpoints.parking.parkingLocationSearch}?${new URLSearchParams({ query })}`,
  );
}

export function reverseParkingLocation(latitude: number, longitude: number) {
  return apiClient<ApiResponse<ParkingLocationResponse[]>>(
    `${apiEndpoints.parking.parkingLocationReverse}?${new URLSearchParams({
      latitude: String(latitude),
      longitude: String(longitude),
    })}`,
  );
}
