import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = { data: T; message: string; success: boolean; timestamp: string };

export type ParkingLotReview = {
  parkingLotName: string | null;
  reviewerDisplayName: string;
  reviewerCustomerId?: string | null;
  rating: number;
  comment: string | null;
  verified: boolean;
  createdAt: string;
  updatedAt: string | null;
  edited: boolean;
};

export type ReviewSummary = {
  averageRating: number | null;
  reviewCount: number;
  distribution: Record<number, number>;
};

export type ParkingLotReviewPage = {
  summary: ReviewSummary;
  content: ParkingLotReview[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type MyParkingLotReview = {
  eligible: boolean;
  reasonCode: string;
  parkingLotId: string;
  parkingLotName: string;
  currentReview: null | {
    rating: number;
    comment: string | null;
    createdAt: string;
    updatedAt: string | null;
    edited: boolean;
  };
};

function queryString(values: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const serialized = query.toString();
  return serialized ? `?${serialized}` : "";
}

export function getPublicParkingLotReviews(parkingLotId: string, filter: { page?: number; size?: number; rating?: number } = {}) {
  return apiClient<ApiResponse<ParkingLotReviewPage>>(
    `${apiEndpoints.public.parkingLotReviews(parkingLotId)}${queryString(filter)}`,
    { skipAuth: true },
  );
}

export function getMyParkingLotReview(parkingLotId: string) {
  return apiClient<ApiResponse<MyParkingLotReview>>(apiEndpoints.parking.myParkingLotReview(parkingLotId));
}

export function upsertMyParkingLotReview(parkingLotId: string, payload: { rating: number; comment?: string }) {
  return apiClient<ApiResponse<MyParkingLotReview["currentReview"]>>(
    apiEndpoints.parking.myParkingLotReview(parkingLotId),
    { method: "PUT", body: payload },
  );
}

export function getManagedParkingLotReviews(filter: {
  parkingLotId?: string;
  rating?: number;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
} = {}) {
  return apiClient<ApiResponse<ParkingLotReviewPage>>(
    `${apiEndpoints.parking.parkingLotReviews}${queryString(filter)}`,
  );
}
