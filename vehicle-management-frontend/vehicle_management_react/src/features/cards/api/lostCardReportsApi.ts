import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type LostCardReportStatus = "OPEN" | "RESOLVED" | "CANCELLED";

export type LostCardReportContext =
  | "VISITOR_IN_PARKING"
  | "REGISTERED_IN_PARKING"
  | "REGISTERED_OUTSIDE";

export type LostCardReportResponse = {
  lostCardReportId: string;
  reportCode: string;
  cardId: string | null;
  customerId: string | null;
  parkingSessionId: string | null;
  subscriptionId: string | null;
  licensePlate: string | null;
  notificationTime: string;
  timeOfLost: string;
  ticketPrice: number;
  lostCardFee: number;
  totalAmount: number;
  reporterName: string;
  reporterPhone: string;
  identifyCard: string | null;
  registrationLicense: string | null;
  context: LostCardReportContext;
  status: LostCardReportStatus;
  invoiceId: string | null;
  invoiceNo: string | null;
  invoiceStatus: "UNPAID" | "PAID" | "CANCELLED" | "REFUNDED" | null;
  createdAt: string;
  createdBy: string | null;
  updatedAt: string;
  updatedBy: string | null;
};

export type LostCardReportFilter = {
  status?: LostCardReportStatus;
  context?: LostCardReportContext;
  customerId?: string;
  cardId?: string;
  parkingSessionId?: string;
  subscriptionId?: string;
  fromDate?: string;
  toDate?: string;
  keyword?: string;
};

export type LostCardReportSummaryResponse = {
  openCount: number;
  unpaidInvoiceCount: number;
  resolvedCount: number;
  lostCardCount: number;
};

export type LostCardPreviewResponse = {
  context: LostCardReportContext;
  parkingSession: LostCardParkingSessionResponse | null;
  subscription: LostCardSubscriptionResponse | null;
  cardId: string | null;
  customerId: string | null;
  customerVehicleId: string | null;
  ticketPrice: number;
  lostCardFee: number;
  totalAmount: number;
  oldCardNumber: string | null;
  customerName: string | null;
  licensePlate: string | null;
  checkInLicensePlateImagePath: string | null;
  checkInPersonImagePath: string | null;
};

export type CreateLostCardReportRequest = {
  parkingSessionId?: string | null;
  subscriptionId?: string | null;
  timeOfLost: string;
  reporterName: string;
  reporterPhone: string;
  identifyCard?: string | null;
  registrationLicense?: string | null;
  note?: string | null;
};

export type LostCardReportDetailReportResponse = {
  lostCardReportId: string;
  cardId: string | null;
  customerId: string | null;
  parkingSessionId: string | null;
  subscriptionId: string | null;
  notificationTime: string;
  timeOfLost: string;
  ticketPrice: number;
  lostCardFee: number;
  reporterName: string | null;
  reporterPhone: string | null;
  identifyCard: string | null;
  registrationLicense: string | null;
  note: string | null;
  context: LostCardReportContext;
  status: LostCardReportStatus;
  resolvedBy: string | null;
  resolvedAt: string | null;
  cancelledBy: string | null;
  cancelledAt: string | null;
  cancelReason: string | null;
  createdAt: string;
  createdBy: string | null;
  updatedAt: string;
  updatedBy: string | null;
};

export type LostCardParkingSessionResponse = {
  parkingSessionId: string;
  cardId: string | null;
  customerId: string | null;
  customerVehicleId: string | null;
  vehicleTypeId: string | null;
  zoneId: string | null;
  licensePlateIn: string | null;
  licensePlateOut: string | null;
  checkInTime: string | null;
  checkOutTime: string | null;
  status: string | null;
  totalPrice: number | null;
};

export type LostCardSubscriptionResponse = {
  subscriptionId: string;
  customerId: string | null;
  customerVehicleId: string | null;
  cardId: string | null;
  ticketTypeId: string | null;
  priceRuleId: string | null;
  requestedEffectiveFrom: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  price: number | null;
  status: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  rejectedBy: string | null;
  rejectedAt: string | null;
  cardReceiptDate: string | null;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type LostCardPaymentResponse = {
  paymentId: string;
  invoiceId: string;
  paymentMethod: string | null;
  amount: number;
  transactionRef: string | null;
  status: string | null;
  paidAt: string | null;
  receivedBy: string | null;
  note: string | null;
};

export type LostCardPaymentMethod = "CASH" | "QR" | "BANK_TRANSFER" | "MOMO" | "VNPAY";

export type RecordLostCardInvoicePaymentRequest = {
  amount: number;
  note?: string;
  paymentMethod: LostCardPaymentMethod;
  transactionRef?: string;
};

export type LostCardInvoiceDetailResponse = {
  invoiceId: string;
  invoiceNo: string;
  customerId: string | null;
  parkingSessionId: string | null;
  subscriptionId: string | null;
  lostCardReportId: string | null;
  amount: number;
  discountAmount: number;
  finalAmount: number;
  status: "UNPAID" | "PAID" | "CANCELLED" | "REFUNDED";
  issuedAt: string | null;
  paidAt: string | null;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
  payments: LostCardPaymentResponse[];
};

export type LostCardReplacementCardResponse = {
  cardId: string;
  cardNumber: string;
  uid: string | null;
  cardTypeId: string | null;
  status: "AVAILABLE" | "ASSIGNED" | "IN_USE" | "LOST" | "BLOCKED" | string;
};

export type LostCardReportDetailResponse = {
  lostCardReport: LostCardReportDetailReportResponse;
  oldCardNumber: string | null;
  customerName: string | null;
  licensePlate: string | null;
  parkingSession: LostCardParkingSessionResponse | null;
  subscription: LostCardSubscriptionResponse | null;
  invoice: LostCardInvoiceDetailResponse | null;
  checkInLicensePlateImagePath: string | null;
  checkInPersonImagePath: string | null;
};

export type LostCardReportWorkflowResponse = {
  lostCardReport: LostCardReportDetailReportResponse;
  parkingSession: LostCardParkingSessionResponse | null;
  subscription: LostCardSubscriptionResponse | null;
  invoice: LostCardInvoiceDetailResponse | null;
  barrierAction: string | null;
};

export async function getLostCardReports(filter: LostCardReportFilter = {}) {
  const params = new URLSearchParams();

  Object.entries(filter).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });

  const query = params.toString();

  return apiClient<ApiResponse<LostCardReportResponse[]>>(
    `${apiEndpoints.accessControl.lostCardReports}${query ? `?${query}` : ""}`,
  );
}

export async function getLostCardReportSummary(filter: Pick<LostCardReportFilter, "fromDate" | "toDate"> = {}) {
  const params = new URLSearchParams();

  Object.entries(filter).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });

  const query = params.toString();

  return apiClient<ApiResponse<LostCardReportSummaryResponse>>(
    `${apiEndpoints.accessControl.lostCardReports}/summary${query ? `?${query}` : ""}`,
  );
}

export async function getLostCardReportById(lostCardReportId: string) {
  return apiClient<ApiResponse<LostCardReportDetailResponse>>(
    `${apiEndpoints.accessControl.lostCardReports}/${lostCardReportId}`,
  );
}

export async function previewLostCardReport(licensePlate: string) {
  const params = new URLSearchParams({ licensePlate });

  return apiClient<ApiResponse<LostCardPreviewResponse>>(
    `${apiEndpoints.accessControl.lostCardReports}/preview?${params.toString()}`,
  );
}

export async function createLostCardReport(payload: CreateLostCardReportRequest) {
  return apiClient<ApiResponse<LostCardReportWorkflowResponse>>(
    apiEndpoints.accessControl.lostCardReports,
    {
      method: "POST",
      body: payload,
    },
  );
}

export async function getAvailableReplacementCards(lostCardReportId: string) {
  return apiClient<ApiResponse<LostCardReplacementCardResponse[]>>(
    `${apiEndpoints.accessControl.lostCardReports}/${lostCardReportId}/replacement-cards`,
  );
}

export async function recordLostCardInvoicePayment(invoiceId: string, payload: RecordLostCardInvoicePaymentRequest) {
  return apiClient<ApiResponse<LostCardPaymentResponse>>(
    `${apiEndpoints.billing.invoices}/${invoiceId}/payments`,
    {
      method: "POST",
      body: payload,
    },
  );
}

export async function resolveLostCardReport(lostCardReportId: string, newCardId?: string) {
  return apiClient<ApiResponse<unknown>>(
    `${apiEndpoints.accessControl.lostCardReports}/${lostCardReportId}/resolve`,
    {
      method: "PATCH",
      body: newCardId ? { newCardId } : {},
    },
  );
}

export async function cancelLostCardReport(lostCardReportId: string, cancelReason: string) {
  return apiClient<ApiResponse<LostCardReportWorkflowResponse>>(
    `${apiEndpoints.accessControl.lostCardReports}/${lostCardReportId}/cancel`,
    {
      method: "PATCH",
      body: { cancelReason },
    },
  );
}
