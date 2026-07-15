import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import {
  getMyAccountProfile,
  updateMyAccountProfile,
  type AccountProfileStatusResponse,
  type UpdateAccountProfileRequest,
} from "@/features/iam/api/accountProfileApi";
import {
  createCustomerVehicle,
  fetchCustomerVehicles,
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
  | "PENDING_CARD"
  | "ACTIVE"
  | "EXPIRED"
  | "CANCELLED"
  | "REJECTED";

export type CustomerPortalSubscription = {
  approvedAt?: string | null;
  approvedBy?: string | null;
  cardId?: string | null;
  cardReceiptDate?: string | null;
  createdAt?: string | null;
  createdBy?: string | null;
  customerId: string;
  customerVehicleId: string;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  price?: number | string | null;
  priceRuleId?: string | null;
  rejectedAt?: string | null;
  rejectedBy?: string | null;
  rejectionReason?: string | null;
  requestedEffectiveFrom?: string | null;
  status: CustomerPortalSubscriptionStatus;
  subscriptionId: string;
  ticketTypeId: string;
  updatedAt?: string | null;
  updatedBy?: string | null;
};

export type CreateMySubscriptionRequest = {
  customerVehicleId: string;
  requestedEffectiveFrom: string;
  ticketTypeId: string;
};

export type CustomerPortalParkingSession = ParkingSessionManagementResponse;

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

export async function getMyCustomerVehicles(profile: CustomerPortalProfile) {
  return fetchCustomerVehicles(requireCustomerId(profile));
}

export async function saveMyCustomerVehicle(profile: CustomerPortalProfile, payload: CustomerVehiclePayload) {
  return createCustomerVehicle(requireCustomerId(profile), payload);
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

export async function getMySubscriptions(profile: CustomerPortalProfile) {
  const response = await apiClient<ApiResponse<CustomerPortalSubscription[]>>(
    `${apiEndpoints.accessControl.subscriptions}${buildQuery({ customerId: requireCustomerId(profile) })}`,
  );
  return response.data ?? [];
}

export async function createMySubscription(payload: CreateMySubscriptionRequest) {
  const response = await apiClient<ApiResponse<CustomerPortalSubscription>>(
    `${apiEndpoints.accessControl.subscriptions}/me`,
    {
      body: payload,
      method: "POST",
    },
  );
  return response.data;
}

export async function getCustomerPortalLookups() {
  const [vehicleTypesResponse, ticketTypesResponse, priceRulesResponse] = await Promise.all([
    getPublicPricingVehicleTypes(),
    getPublicPricingTicketTypes(),
    getPublicPriceRules({ isActive: true }),
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
  return response.data ?? [];
}
