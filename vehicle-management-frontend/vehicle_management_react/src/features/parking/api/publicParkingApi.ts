import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type NearbyParkingLot = {
  parkingLotId: string;
  name: string;
  address: string | null;
  latitude: number;
  longitude: number;
  distanceMeters: number;
  distanceKm: number;
};

export type PublicParkingLocation = {
  displayName: string;
  latitude: number;
  longitude: number;
};

export function getNearbyParkingLots(latitude: number, longitude: number, radiusKm: number, limit = 20) {
  const query = new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    radiusKm: String(radiusKm),
    limit: String(limit),
  });

  return apiClient<ApiResponse<NearbyParkingLot[]>>(
    `${apiEndpoints.public.nearbyParkingLots}?${query}`,
    { skipAuth: true },
  );
}

export function searchPublicParkingLocations(query: string) {
  return apiClient<ApiResponse<PublicParkingLocation[]>>(
    `${apiEndpoints.public.parkingLocationSearch}?${new URLSearchParams({ query })}`,
    { skipAuth: true },
  );
}

export function reversePublicParkingLocation(latitude: number, longitude: number) {
  return apiClient<ApiResponse<PublicParkingLocation[]>>(
    `${apiEndpoints.public.parkingLocationReverse}?${new URLSearchParams({
      latitude: String(latitude),
      longitude: String(longitude),
    })}`,
    { skipAuth: true },
  );
}
