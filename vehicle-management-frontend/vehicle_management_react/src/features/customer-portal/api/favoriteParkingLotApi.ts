import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

type FavoriteParkingLotListResponse = {
  parkingLotIds: string[];
};

export type FavoriteParkingLotResponse = {
  parkingLotId: string;
  favorite: boolean;
};

export async function getMyFavoriteParkingLotIds() {
  const response = await apiClient<ApiResponse<FavoriteParkingLotListResponse>>(
    apiEndpoints.people.customerFavoriteParkingLots,
  );
  return new Set(response.data.parkingLotIds ?? []);
}

export async function setMyFavoriteParkingLot(parkingLotId: string, favorite: boolean) {
  const response = await apiClient<ApiResponse<FavoriteParkingLotResponse>>(
    `${apiEndpoints.people.customerFavoriteParkingLots}/${encodeURIComponent(parkingLotId)}`,
    {
      body: { favorite },
      method: "PUT",
    },
  );
  return response.data;
}
