import { appConfig } from "@/config/env";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { apiClient } from "@/core/api/apiClient";
import { getValidAccessToken, refreshAccessToken } from "@/core/auth/tokenRefresh";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type LaneDirection = "IN" | "OUT";
export type LaneStatus = "ACTIVE" | "MAINTENANCE" | "CLOSED";
export type ParkingCardStatus = "AVAILABLE" | "ASSIGNED" | "IN_USE" | "BLOCKED" | "LOST" | "DAMAGED" | "RETIRED" | "RESERVED";
export type ZoneStatus = "ACTIVE" | "MAINTENANCE" | "CLOSED";

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

export type ZoneResponse = {
  capacity?: number;
  code: string;
  name: string;
  parkingLotId?: string;
  status: ZoneStatus;
  vehicleTypeId?: string;
  zoneId: string;
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

export type ParkingSessionManagementEventResponse = {
  actorAccountId?: string;
  eventTime?: string;
  eventType?: "CHECK_IN" | "CHECK_OUT";
  laneCode?: string;
  laneId?: string;
  laneName?: string;
  licensePlateDetected?: string;
  licensePlateImagePath?: string;
  note?: string;
  parkingEventId: string;
  parkingSessionId: string;
  personImagePath?: string;
};

export type ParkingSessionManagementResponse = {
  cardId?: string;
  cardNumber?: string;
  cardTypeCode?: string;
  cardTypeName?: string;
  cardUid?: string;
  checkInTime?: string;
  checkOutTime?: string;
  customerId?: string;
  customerVehicleId?: string;
  events?: ParkingSessionManagementEventResponse[];
  licensePlateIn?: string;
  licensePlateOut?: string;
  parkingLotCode?: string;
  parkingLotId?: string;
  parkingLotName?: string;
  parkingSessionId: string;
  status: "OPEN" | "CLOSED" | "LOST_CARD";
  totalPrice?: number;
  vehicleTypeCode?: string;
  vehicleTypeId?: string;
  vehicleTypeName?: string;
  zoneCode?: string;
  zoneId?: string;
  zoneName?: string;
};

export type ParkingSessionManagementFilters = {
  fromDate?: string;
  keyword?: string;
  status?: ParkingSessionResponse["status"];
  toDate?: string;
  vehicleTypeId?: string;
  zoneId?: string;
};

export type LicensePlateOcrBoundingBox = {
  x1?: number | null;
  y1?: number | null;
  x2?: number | null;
  y2?: number | null;
};

export type LicensePlateOcrCandidate = {
  bbox?: LicensePlateOcrBoundingBox | null;
  confidence?: number | null;
  correctionCount?: number | null;
  detectorConfidence?: number | null;
  formattedLicensePlate?: string | null;
  licensePlate: string;
  normalizedLicensePlate: string;
  ocrConfidence?: number | null;
  plateType?: string | null;
  validFormat?: boolean | null;
};

export type LicensePlateOcrDetection = {
  bbox?: LicensePlateOcrBoundingBox | null;
  classId?: number | null;
  confidence?: number | null;
};

export type LicensePlateOcrResponse = {
  bbox?: LicensePlateOcrBoundingBox | null;
  candidates: LicensePlateOcrCandidate[];
  confidence?: number | null;
  correctionCount?: number | null;
  detections: LicensePlateOcrDetection[];
  detectorConfidence?: number | null;
  formattedLicensePlate?: string | null;
  licensePlate: string;
  modelStage?: string | null;
  modelVersion?: string | null;
  needsReview: boolean;
  normalizedLicensePlate: string;
  ocrConfidence?: number | null;
  plateType?: string | null;
  processingMs?: number | null;
  rawResponse?: Record<string, unknown>;
  requestId?: string | null;
  reviewReasons: string[];
  validFormat?: boolean | null;
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

export async function fetchParkingZones(status?: ZoneStatus) {
  const query = new URLSearchParams();
  if (status) query.set("status", status);

  const suffix = query.toString() ? `?${query.toString()}` : "";
  const response = await apiClient<ApiResponse<ZoneResponse[]>>(
    `${apiEndpoints.parking.zones}${suffix}`,
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

export async function fetchParkingSessions(filters: ParkingSessionManagementFilters = {}) {
  const query = new URLSearchParams();
  if (filters.status) query.set("status", filters.status);
  if (filters.fromDate) query.set("fromDate", filters.fromDate);
  if (filters.toDate) query.set("toDate", filters.toDate);
  if (filters.vehicleTypeId) query.set("vehicleTypeId", filters.vehicleTypeId);
  if (filters.zoneId) query.set("zoneId", filters.zoneId);
  if (filters.keyword?.trim()) query.set("keyword", filters.keyword.trim());

  const suffix = query.toString() ? `?${query.toString()}` : "";
  const response = await apiClient<ApiResponse<ParkingSessionManagementResponse[]>>(
    `${apiEndpoints.parking.parkingSessions}${suffix}`,
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

export async function recognizeLicensePlate(
  image: File,
  context: { direction?: LaneDirection; laneId?: string } = {},
) {
  const formData = new FormData();
  formData.append("image", image, image.name);
  if (context.laneId) formData.append("laneId", context.laneId);
  if (context.direction) formData.append("direction", context.direction);

  return postMultipart<ApiResponse<LicensePlateOcrResponse>>(
    apiEndpoints.parking.ocrLicensePlate,
    formData,
  );
}

async function postMultipart<T>(path: string, body: FormData): Promise<T> {
  const accessToken = await getValidAccessToken();
  let response = await sendMultipartRequest(path, body, accessToken);

  if (response.status === 401 && accessToken) {
    const refreshedToken = await refreshAccessToken();
    if (refreshedToken) {
      response = await sendMultipartRequest(path, body, refreshedToken);
    }
  }

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

function sendMultipartRequest(path: string, body: FormData, accessToken: string | null) {
  return fetch(`${appConfig.apiBaseUrl}${path}`, {
    body,
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined,
    method: "POST",
  });
}
