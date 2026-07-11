import { appConfig } from "@/config/env";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { apiClient } from "@/core/api/apiClient";
import { getAccessToken } from "@/core/auth/session";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type LaneDirection = "IN" | "OUT";
export type LaneStatus = "ACTIVE" | "MAINTENANCE" | "CLOSED";
export type ParkingCardStatus = "AVAILABLE" | "ASSIGNED" | "IN_USE" | "BLOCKED" | "LOST" | "DAMAGED" | "RETIRED" | "RESERVED";

export type LaneResponse = {
  code: string;
  createdAt?: string;
  direction: LaneDirection;
  gateId?: string | null;
  laneId: string;
  name: string;
  status: LaneStatus;
  updatedAt?: string;
};

export type ParkingCardResponse = {
  blockedAt?: string;
  blockedReason?: string;
  cardId: string;
  cardNumber: string;
  cardTypeId?: string;
  createdAt?: string;
  issuedAt?: string;
  registeredVehicleTypeId?: string;
  status: ParkingCardStatus;
  uid: string;
  updatedAt?: string;
};

export type CardTypeResponse = {
  cardTypeId: string;
  code: string;
  description?: string;
  isActive?: boolean;
  isReturnRequired?: boolean;
  name: string;
};

export type VehicleTypeResponse = {
  code: string;
  description?: string;
  isActive?: boolean;
  name: string;
  vehicleTypeId: string;
};

export type CheckInParkingSessionRequest = {
  cardUid: string;
  laneId: string;
  licensePlate: string;
  note?: string;
  vehicleTypeId?: string;
};

export type CheckOutParkingSessionRequest = {
  cardUid: string;
  laneId: string;
  licensePlate: string;
  note?: string;
};

export type ParkingSessionResponse = {
  cardId?: string;
  checkInTime?: string;
  checkOutTime?: string;
  customerId?: string;
  customerVehicleId?: string;
  licensePlateIn?: string;
  licensePlateOut?: string;
  parkingSessionId: string;
  status: "OPEN" | "CLOSED" | "LOST_CARD";
  totalPrice?: number;
  vehicleTypeId?: string;
  zoneId?: string;
};

export type ParkingEventResponse = {
  actorAccountId?: string;
  eventTime?: string;
  eventType?: "CHECK_IN" | "CHECK_OUT";
  laneId: string;
  licensePlateDetected?: string;
  licensePlateImagePath?: string;
  note?: string;
  parkingEventId: string;
  parkingSessionId: string;
  personImagePath?: string;
};

export type ParkingSessionCheckInResponse = {
  barrierAction: "OPEN" | string;
  customerType: "VISITOR" | "SUBSCRIPTION" | string;
  parkingEvent: ParkingEventResponse;
  parkingSession: ParkingSessionResponse;
  subscriptionId?: string;
};

export type InvoiceAdminResponse = {
  amount?: number;
  customerId?: string;
  discountAmount?: number;
  finalAmount?: number;
  invoiceId: string;
  invoiceNo?: string;
  issuedAt?: string;
  paidAt?: string;
  parkingSessionId?: string;
  status?: "UNPAID" | "PAID" | "CANCELLED" | string;
  subscriptionId?: string;
};

export type ParkingSessionCheckOutResponse = {
  barrierAction: "OPEN" | "WAIT_PAYMENT" | string;
  customerType: "VISITOR" | "SUBSCRIPTION" | string;
  invoice?: InvoiceAdminResponse | null;
  parkingEvent: ParkingEventResponse;
  parkingSession: ParkingSessionResponse;
};

export type ParkingSessionOperationResponse = ParkingSessionCheckInResponse | ParkingSessionCheckOutResponse;

export type ParkingSessionCheckOutPreviewResponse = {
  checkInEvent?: ParkingEventResponse | null;
  customerType: "VISITOR" | "SUBSCRIPTION" | string;
  estimatedTotalPrice?: number;
  parkingSession: ParkingSessionResponse;
  pricingMessage?: string;
  previewCheckOutTime?: string;
};

export type LicensePlateOcrCandidate = {
  confidence: number;
  detectorConfidence: number;
  licensePlate: string;
  normalizedLicensePlate: string;
  ocrConfidence: number;
};

export type LicensePlateOcrResponse = {
  candidates: LicensePlateOcrCandidate[];
  confidence: number;
  detectorConfidence: number;
  licensePlate: string;
  needsReview: boolean;
  normalizedLicensePlate: string;
  ocrConfidence: number;
};

export async function fetchParkingLanes(direction?: LaneDirection) {
  const query = new URLSearchParams();
  if (direction) query.set("direction", direction);
  query.set("status", "ACTIVE");

  const response = await apiClient<ApiResponse<LaneResponse[]>>(
    `${apiEndpoints.parking.lanes}?${query.toString()}`,
  );

  return response.data;
}

export async function fetchParkingCards(status?: ParkingCardStatus) {
  const query = new URLSearchParams();
  if (status) query.set("status", status);

  const suffix = query.toString() ? `?${query.toString()}` : "";
  const response = await apiClient<ApiResponse<ParkingCardResponse[]>>(
    `${apiEndpoints.cards.cards}${suffix}`,
  );

  return response.data;
}

export async function fetchCardTypes() {
  const query = new URLSearchParams();
  query.set("isActive", "true");

  const response = await apiClient<ApiResponse<CardTypeResponse[]>>(
    `${apiEndpoints.catalog.cardTypes}?${query.toString()}`,
  );

  return response.data;
}

export async function fetchVehicleTypes() {
  const query = new URLSearchParams();
  query.set("isActive", "true");

  const response = await apiClient<ApiResponse<VehicleTypeResponse[]>>(
    `${apiEndpoints.catalog.vehicleTypes}?${query.toString()}`,
  );

  return response.data;
}

export async function fetchOpenParkingSessionByCardUid(cardUid: string) {
  const query = new URLSearchParams();
  query.set("cardUid", cardUid);

  const response = await apiClient<ApiResponse<ParkingSessionCheckOutPreviewResponse>>(
    `${apiEndpoints.parking.parkingSessions}/open-by-card?${query.toString()}`,
  );

  return response.data;
}

export async function checkInParkingSession(
  request: CheckInParkingSessionRequest,
  licensePlateImage: File,
  personImage: File,
) {
  const formData = new FormData();
  formData.append("request", JSON.stringify(request));
  formData.append("licensePlateImage", licensePlateImage, licensePlateImage.name);
  formData.append("personImage", personImage, personImage.name);

  return postMultipart<ApiResponse<ParkingSessionCheckInResponse>>(
    `${apiEndpoints.parking.parkingSessions}/check-in`,
    formData,
  );
}

export async function checkOutParkingSession(
  request: CheckOutParkingSessionRequest,
  licensePlateImage: File,
  personImage: File,
) {
  const formData = new FormData();
  formData.append("request", JSON.stringify(request));
  formData.append("licensePlateImage", licensePlateImage, licensePlateImage.name);
  formData.append("personImage", personImage, personImage.name);

  return postMultipart<ApiResponse<ParkingSessionCheckOutResponse>>(
    `${apiEndpoints.parking.parkingSessions}/check-out`,
    formData,
  );
}

export async function recognizeLicensePlate(image: File) {
  const formData = new FormData();
  formData.append("image", image, image.name);

  return postMultipart<ApiResponse<LicensePlateOcrResponse>>(
    apiEndpoints.parking.ocrLicensePlate,
    formData,
  );
}

async function postMultipart<T>(path: string, body: FormData): Promise<T> {
  const accessToken = getAccessToken();
  const response = await fetch(`${appConfig.apiBaseUrl}${path}`, {
    body,
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined,
    method: "POST",
  });

  const contentType = response.headers.get("content-type") ?? "";
  const responseBody = contentType.includes("application/json") ? await response.json() : null;

  if (!response.ok) {
    const message =
      responseBody &&
      typeof responseBody === "object" &&
      "message" in responseBody &&
      typeof responseBody.message === "string"
        ? responseBody.message
        : `API error ${response.status}`;

    throw new Error(message);
  }

  return responseBody as T;
}
