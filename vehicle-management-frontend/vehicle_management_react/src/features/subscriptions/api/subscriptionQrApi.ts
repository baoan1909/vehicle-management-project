import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type SubscriptionQrCredential = {
  subscriptionId: string;
  parkingLotId: string;
  customerVehicleId: string;
  qrPayload: string;
  qrCodeDataUrl: string;
  issuedAt: string;
  effectiveFrom: string;
  effectiveTo: string;
};

export type SubscriptionQrVerificationStatus =
  | "VALID"
  | "INVALID_QR"
  | "NOT_YET_VALID"
  | "EXPIRED"
  | "REVOKED"
  | "INACTIVE"
  | "WRONG_PARKING_LOT"
  | "VEHICLE_MISMATCH";

export type SubscriptionQrVerification = {
  status: SubscriptionQrVerificationStatus;
  message: string;
  subscriptionId?: string | null;
  parkingLotId?: string | null;
  customerVehicleId?: string | null;
  licensePlate?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
};

export async function issueMySubscriptionQr(subscriptionId: string) {
  const response = await apiClient<ApiResponse<SubscriptionQrCredential>>(
    `${apiEndpoints.accessControl.subscriptions}/me/${subscriptionId}/qr`,
    { method: "POST" },
  );
  return response.data;
}

export async function verifySubscriptionQr(payload: {
  qrPayload: string;
  parkingLotId: string;
  licensePlate?: string;
}) {
  const response = await apiClient<ApiResponse<SubscriptionQrVerification>>(
    `${apiEndpoints.accessControl.subscriptions}/qr/verify`,
    { body: payload, method: "POST" },
  );
  return response.data;
}

export async function verifySubscriptionQrImage(payload: {
  image: File;
  parkingLotId: string;
  licensePlate?: string;
}) {
  const formData = new FormData();
  formData.append("image", payload.image, payload.image.name);
  formData.append("parkingLotId", payload.parkingLotId);
  if (payload.licensePlate) formData.append("licensePlate", payload.licensePlate);

  const response = await apiClient<ApiResponse<SubscriptionQrVerification>>(
    `${apiEndpoints.accessControl.subscriptions}/qr/verify-image`,
    { body: formData, method: "POST" },
  );
  return response.data;
}
