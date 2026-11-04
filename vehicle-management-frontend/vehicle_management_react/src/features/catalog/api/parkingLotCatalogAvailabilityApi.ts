import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type ParkingLotCatalogAvailability = {
  parkingLotId: string;
  organizationId: string;
  excludedVehicleTypeIds: string[];
  drainingVehicleTypeIds: string[];
  excludedTicketTypeIds: string[];
};

export function getParkingLotCatalogAvailability(parkingLotId: string) {
  return apiClient<ApiResponse<ParkingLotCatalogAvailability>>(
    apiEndpoints.catalog.parkingLotCatalogAvailability(parkingLotId),
  );
}

export function setParkingLotVehicleTypeEnabled(parkingLotId: string, vehicleTypeId: string, enabled: boolean) {
  return apiClient<ApiResponse<ParkingLotCatalogAvailability>>(
    `${apiEndpoints.catalog.parkingLotCatalogAvailability(parkingLotId)}/vehicle-types/${vehicleTypeId}`,
    { method: "PUT", body: { enabled } },
  );
}

export function setParkingLotVehicleTypeEnabledForLots(
  parkingLotIds: string[],
  vehicleTypeId: string,
  enabled: boolean,
) {
  return apiClient<ApiResponse<ParkingLotCatalogAvailability[]>>(
    `/catalog/parking-lots/availability/vehicle-types/${vehicleTypeId}`,
    { method: "PUT", body: { parkingLotIds, enabled } },
  );
}

export function setParkingLotTicketTypeEnabled(parkingLotId: string, ticketTypeId: string, enabled: boolean) {
  return apiClient<ApiResponse<ParkingLotCatalogAvailability>>(
    `${apiEndpoints.catalog.parkingLotCatalogAvailability(parkingLotId)}/ticket-types/${ticketTypeId}`,
    { method: "PUT", body: { enabled } },
  );
}
