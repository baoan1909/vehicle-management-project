import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type ParkingMapFeatureStatus = {
  customerParkingMapEnabled: boolean;
  publicNearbySearchEnabled: boolean;
  geocodingProviderEnabled: boolean;
};

export function getParkingMapFeatures() {
  return apiClient<ApiResponse<ParkingMapFeatureStatus>>(apiEndpoints.public.parkingMapFeatures, {
    skipAuth: true,
  });
}
export type NearbyParkingLot = {
  parkingLotId: string;
  name: string;
  addressDisplay: string | null;
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
  );
}

export function searchPublicParkingLocations(query: string) {
  return apiClient<ApiResponse<PublicParkingLocation[]>>(
    `${apiEndpoints.public.parkingLocationSearch}?${new URLSearchParams({ query })}`,
  );
}

export function reversePublicParkingLocation(latitude: number, longitude: number) {
  return apiClient<ApiResponse<PublicParkingLocation[]>>(
    `${apiEndpoints.public.parkingLocationReverse}?${new URLSearchParams({
      latitude: String(latitude),
      longitude: String(longitude),
    })}`,
  );
}

export type GeolocationOutcome =
  | "GRANTED"
  | "DENIED"
  | "TIMEOUT"
  | "UNAVAILABLE"
  | "UNSUPPORTED"
  | "INSECURE_CONTEXT"
  | "LOW_ACCURACY";

export function recordGeolocationOutcome(outcome: GeolocationOutcome) {
  return apiClient<ApiResponse<void>>(apiEndpoints.public.geolocationTelemetry, {
    body: { outcome },
    method: "POST",
    keepalive: true,
  });
}
