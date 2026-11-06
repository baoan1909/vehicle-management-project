import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import {
  getMyAccountProfile,
  updateMyAccountProfile,
  type AccountProfileStatusResponse,
  type UpdateAccountProfileRequest,
} from "@/features/iam/api/accountProfileApi";
import {
  type CustomerVehicleAdminResponse,
  type CustomerVehiclePayload,
} from "@/features/customers/api/customerApi";
import {
  getPublicPriceRules,
  getPublicPricingTicketTypes,
  getPublicPricingVehicleTypes,
  type PriceRuleApiResponse,
  type TicketTypeApiResponse,
  type VehicleTypeApiResponse,
} from "@/features/pricing/api/pricingApi";
import type {
  ParkingSessionManagementFilters,
  ParkingSessionManagementResponse,
} from "@/features/parking/api/parkingSessionApi";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type CustomerPortalProfile = AccountProfileStatusResponse;
export type CustomerPortalVehicle = CustomerVehicleAdminResponse;
export type CustomerPortalVehiclePayload = CustomerVehiclePayload;
export type CustomerPortalVehicleType = VehicleTypeApiResponse;
export type CustomerPortalTicketType = TicketTypeApiResponse;
export type CustomerPortalPriceRule = PriceRuleApiResponse;

export type CustomerPortalSubscriptionStatus =
  | "PENDING"
  | "PENDING_PAYMENT"
  | "PENDING_SPACE"
  | "ACTIVE"
  | "EXPIRED"
  | "CANCELLED"
  | "REJECTED";

export type CustomerPortalPhysicalCardStatus =
  | "NOT_REQUESTED"
  | "PAYMENT_PENDING"
  | "RESERVED"
  | "WAITING_INVENTORY"
  | "ASSIGNED";

export type CustomerPortalSubscription = {
  parkingLotId?: string | null;
  approvedAt?: string | null;
  approvedBy?: string | null;
  cardId?: string | null;
  cardReceiptDate?: string | null;
  accessBlocked?: boolean | null;
  accessBlockedAt?: string | null;
  accessRestoredAt?: string | null;
  createdAt?: string | null;
  createdBy?: string | null;
  customerId: string;
  customerVehicleId: string;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  price?: number | string | null;
  priceRuleId?: string | null;
  periodQuantity?: number | null;
  physicalCardRequested?: boolean | null;
  physicalCardDeposit?: number | string | null;
  physicalCardStatus?: CustomerPortalPhysicalCardStatus | null;
  rejectedAt?: string | null;
  rejectedBy?: string | null;
  rejectionReason?: string | null;
  requestedEffectiveFrom?: string | null;
  requestedVoucherCode?: string | null;
  status: CustomerPortalSubscriptionStatus;
  subscriptionId: string;
  ticketTypeId: string;
  updatedAt?: string | null;
  updatedBy?: string | null;
  spaceSelectionRequired?: boolean | null;
  spaceSelectionConfirmedAt?: string | null;
  spaceSelectionExpiresAt?: string | null;
};

export type CustomerPortalSubscriptionSpace = {
  parkingSpaceId: string;
  code: string;
  status: string;
  x: number | string | null;
  y: number | string | null;
  width: number | string | null;
  height: number | string | null;
  rotation: number | string | null;
  selected: boolean;
  selectable: boolean;
};

export type CustomerPortalSpaceSelection = {
  subscriptionId: string;
  selectedParkingSpaceId: string;
  selectedParkingSpaceCode: string;
  canvasWidth: number | string;
  canvasHeight: number | string;
  selectionExpiresAt?: string | null;
  spaces: CustomerPortalSubscriptionSpace[];
};

export type CreateMySubscriptionRequest = {
  parkingLotId: string;
  customerVehicleId: string;
  requestedEffectiveFrom: string;
  ticketTypeId: string;
  voucherCode?: string;
  periodQuantity: number;
  physicalCardRequested: boolean;
};

export type CustomerPortalSubscriptionVoucherQuote = {
  voucherCode: string | null;
  baseAmount: number | string;
  discountAmount: number | string;
  finalAmount: number | string;
};

export type CustomerPortalVoucherBanner = {
  code: string;
  bannerTitle: string;
  bannerDescription: string | null;
  bannerPriority: number;
  showOnDashboard: boolean;
  showOnSubscriptionPage: boolean;
};

export type CustomerPortalParkingSession = ParkingSessionManagementResponse;

function normalizeParkingSession(session: CustomerPortalParkingSession): CustomerPortalParkingSession {
  const checkInEvent = session.events?.find((event) => event.eventType === "CHECK_IN");
  const checkOutEvent = session.events?.find((event) => event.eventType === "CHECK_OUT");

  return {
    ...session,
    checkInTime: session.checkInTime || checkInEvent?.eventTime,
    checkOutTime: session.checkOutTime || checkOutEvent?.eventTime,
    licensePlateIn: session.licensePlateIn || checkInEvent?.licensePlateDetected,
    licensePlateOut: session.licensePlateOut || checkOutEvent?.licensePlateDetected,
  };
}

function buildQuery(filter: Record<string, string | number | boolean | null | undefined>) {
  const params = new URLSearchParams();

  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });

  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function getCustomerPortalProfile() {
  const response = await getMyAccountProfile();
  return response.data;
}

export async function updateCustomerPortalProfile(payload: UpdateAccountProfileRequest) {
  const response = await updateMyAccountProfile(payload);
  return response.data;
}

export function requireCustomerId(profile: CustomerPortalProfile) {
  const customerId = profile.customer?.customerId;
  if (!customerId) {
    throw new Error("Tài khoản hiện tại chưa liên kết hồ sơ khách hàng.");
  }
  return customerId;
}

export async function getMyCustomerVehicles() {
  const response = await apiClient<ApiResponse<CustomerPortalVehicle[]>>(
    `${apiEndpoints.customers.customerVehicles}/me`,
  );
  return response.data ?? [];
}

export async function saveMyCustomerVehicle(payload: CustomerVehiclePayload) {
  const response = await apiClient<ApiResponse<CustomerPortalVehicle>>(
    `${apiEndpoints.customers.customerVehicles}/me`,
    {
      body: payload,
      method: "POST",
    },
  );
  return response.data;
}

export async function updateMyCustomerVehicle(customerVehicleId: string, payload: CustomerVehiclePayload) {
  const response = await apiClient<ApiResponse<CustomerPortalVehicle>>(
    `${apiEndpoints.customers.customerVehicles}/${customerVehicleId}`,
    {
      body: payload,
      method: "PUT",
    },
  );
  return response.data;
}

export async function inactivateMyCustomerVehicle(customerVehicleId: string) {
  const response = await apiClient<ApiResponse<CustomerPortalVehicle>>(
    `${apiEndpoints.customers.customerVehicles}/${customerVehicleId}/inactivate`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function activateMyCustomerVehicle(customerVehicleId: string) {
  const response = await apiClient<ApiResponse<CustomerPortalVehicle>>(
    `${apiEndpoints.customers.customerVehicles}/${customerVehicleId}/activate`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function markMyCustomerVehicleAsDefault(customerVehicleId: string) {
  const response = await apiClient<ApiResponse<CustomerPortalVehicle>>(
    `${apiEndpoints.customers.customerVehicles}/${customerVehicleId}/mark-default`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function getMySubscriptions() {
  const response = await apiClient<ApiResponse<CustomerPortalSubscription[]>>(
    `${apiEndpoints.accessControl.subscriptions}/me`,
  );
  return response.data ?? [];
}

export async function createMySubscription(payload: CreateMySubscriptionRequest, idempotencyKey?: string) {
  const response = await apiClient<ApiResponse<CustomerPortalSubscription>>(
    `${apiEndpoints.accessControl.subscriptions}/me`,
    {
      body: payload,
      headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
      method: "POST",
    },
  );
  return response.data;
}

export async function getMySubscriptionSpaceSelection(subscriptionId: string) {
  const response = await apiClient<ApiResponse<CustomerPortalSpaceSelection>>(
    `${apiEndpoints.accessControl.subscriptions}/me/${subscriptionId}/space-selection`,
  );
  return response.data;
}

export async function selectMySubscriptionParkingSpace(subscriptionId: string, parkingSpaceId: string) {
  const response = await apiClient<ApiResponse<CustomerPortalSpaceSelection>>(
    `${apiEndpoints.accessControl.subscriptions}/me/${subscriptionId}/space-selection`,
    { body: { parkingSpaceId }, method: "PUT" },
  );
  return response.data;
}

export async function confirmMySubscriptionSpaceSelection(subscriptionId: string) {
  const response = await apiClient<ApiResponse<CustomerPortalSubscription>>(
    `${apiEndpoints.accessControl.subscriptions}/me/${subscriptionId}/space-selection/confirm`,
    { method: "POST" },
  );
  return response.data;
}

export type CustomerPortalParkingLot = {
  parkingLotId: string;
  name: string;
  addressDisplay?: string | null;
};

export async function getCustomerPortalParkingLots() {
  const response = await apiClient<ApiResponse<CustomerPortalParkingLot[]>>(
    apiEndpoints.public.parkingLots,
    { skipAuth: true },
  );
  return response.data ?? [];
}

export async function getCustomerPortalLookups(parkingLotId?: string) {
  const [vehicleTypesResponse, ticketTypesResponse, priceRulesResponse] = await Promise.all([
    getPublicPricingVehicleTypes(parkingLotId),
    getPublicPricingTicketTypes(parkingLotId),
    getPublicPriceRules({ isActive: true, parkingLotId }),
  ]);

  return {
    priceRules: priceRulesResponse.data ?? [],
    ticketTypes: ticketTypesResponse.data ?? [],
    vehicleTypes: vehicleTypesResponse.data ?? [],
  };
}

export async function getMyParkingSessions(filters: ParkingSessionManagementFilters = {}) {
  const response = await apiClient<ApiResponse<CustomerPortalParkingSession[]>>(
    `${apiEndpoints.parking.parkingSessions}/me${buildQuery(filters)}`,
  );
  return (response.data ?? []).map(normalizeParkingSession);
}

export async function quoteMySubscriptionVoucher(payload: CreateMySubscriptionRequest) {
  const response = await apiClient<ApiResponse<CustomerPortalSubscriptionVoucherQuote>>(
    `${apiEndpoints.accessControl.subscriptions}/me/voucher-quote`,
    {
      body: payload,
      method: "POST",
    },
  );
  return response.data;
}

export type CustomerPortalInvoice = {
  amount: number;
  discountAmount: number;
  finalAmount: number;
  invoiceId: string;
  invoiceNo: string;
  issuedAt?: string | null;
  paidAt?: string | null;
  purpose?: CustomerPortalInvoicePurpose | null;
  status: "UNPAID" | "PAID" | "CANCELLED" | "REFUNDED";
  subscriptionId?: string | null;
};

export type CustomerPortalInvoicePurpose =
  | "MANUAL"
  | "PARKING_SESSION"
  | "SUBSCRIPTION_PURCHASE"
  | "SUBSCRIPTION_RENEWAL"
  | "SUBSCRIPTION_CARD_DEPOSIT"
  | "LOST_CARD";

export async function getMySubscriptionInvoices(subscriptionIds: string[]) {
  const params = new URLSearchParams();
  subscriptionIds.filter(Boolean).slice(0, 50).forEach((id) => params.append("subscriptionIds", id));
  const query = params.toString();
  const response = await apiClient<ApiResponse<CustomerPortalInvoice[]>>(
    `${apiEndpoints.billing.invoices}/me${query ? `?${query}` : ""}`,
  );
  return response.data ?? [];
}

export async function getMySubscriptionInvoice(subscriptionId: string) {
  const invoices = await getMySubscriptionInvoices([subscriptionId]);
  const purchaseInvoices = invoices.filter((invoice) => (
    !invoice.purpose || invoice.purpose === "SUBSCRIPTION_PURCHASE"
  ));
  return (
    purchaseInvoices.find((invoice) => invoice.status === "UNPAID")
    ?? purchaseInvoices.find((invoice) => invoice.status === "PAID")
    ?? purchaseInvoices[0]
    ?? null
  );
}

export async function getMySubscriptionCardDepositInvoice(subscriptionId: string) {
  const invoices = await getMySubscriptionInvoices([subscriptionId]);
  const depositInvoices = invoices.filter((invoice) => invoice.purpose === "SUBSCRIPTION_CARD_DEPOSIT");
  return (
    depositInvoices.find((invoice) => invoice.status === "UNPAID")
    ?? depositInvoices.find((invoice) => invoice.status === "PAID")
    ?? depositInvoices[0]
    ?? null
  );
}

export async function requestMySubscriptionPhysicalCard(subscriptionId: string) {
  const response = await apiClient<ApiResponse<CustomerPortalInvoice>>(
    `${apiEndpoints.accessControl.subscriptions}/me/${subscriptionId}/physical-card-request`,
    { method: "POST" },
  );
  return response.data;
}

export async function renewMySubscription(
  subscriptionId: string,
  payload: CustomerPortalSubscriptionRenewalRequest,
  idempotencyKey?: string,
) {
  const response = await apiClient<ApiResponse<CustomerPortalSubscriptionRenewalResult>>(
    `${apiEndpoints.accessControl.subscriptions}/me/${subscriptionId}/renewal`,
    {
      body: payload,
      headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
      method: "POST",
    },
  );
  return response.data;
}

export type CustomerPortalSubscriptionRenewalRequest = {
  ticketTypeId: string;
  periodQuantity: number;
  voucherCode?: string;
  physicalCardRequested: boolean;
};

export type CustomerPortalSubscriptionRenewalQuote = {
  effectiveFrom: string;
  effectiveTo: string;
  ticketAmount: number | string;
  physicalCardDeposit: number | string;
  voucherCode?: string | null;
  discountAmount: number | string;
  finalAmount: number | string;
  reusesExistingCard: boolean;
  physicalCardRequestPending: boolean;
};

export type CustomerPortalSubscriptionRenewal = {
  renewalId: string;
  subscriptionId: string;
  invoiceId: string;
  ticketTypeId: string;
  priceRuleId: string;
  periodQuantity: number;
  effectiveFrom: string;
  effectiveTo: string;
  ticketAmount: number | string;
  physicalCardDeposit: number | string;
  discountAmount: number | string;
  finalAmount: number | string;
  voucherCode?: string | null;
  physicalCardRequested: boolean;
  status: "PENDING_PAYMENT" | "SCHEDULED" | "APPLIED" | "CANCELLED";
};

export type CustomerPortalSubscriptionRenewalResult = {
  renewal: CustomerPortalSubscriptionRenewal;
  invoice: CustomerPortalInvoice;
};

export async function quoteMySubscriptionRenewal(
  subscriptionId: string,
  payload: CustomerPortalSubscriptionRenewalRequest,
) {
  const response = await apiClient<ApiResponse<CustomerPortalSubscriptionRenewalQuote>>(
    `${apiEndpoints.accessControl.subscriptions}/me/${subscriptionId}/renewal/quote`,
    { body: payload, method: "POST" },
  );
  return response.data;
}

export async function getCustomerPortalVoucherBanners(parkingLotId?: string) {
  const query = parkingLotId ? `?parkingLotId=${encodeURIComponent(parkingLotId)}` : "";
  const response = await apiClient<ApiResponse<CustomerPortalVoucherBanner[]>>(
    `${apiEndpoints.catalog.customerVoucherBanners}${query}`,
    { skipAuth: true },
  );
  return response.data ?? [];
}
