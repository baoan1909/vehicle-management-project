import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { fetchCards, type CardResponse } from "@/features/cards/api/cardApi";
import { getTicketTypes, type TicketTypeApiResponse } from "@/features/catalog/api/ticketTypesApi";
import { getVehicleTypes, type VehicleTypeApiResponse } from "@/features/catalog/api/vehicleTypesApi";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type SubscriptionStatus =
  | "PENDING"
  | "PENDING_PAYMENT"
  | "PENDING_CARD"
  | "ACTIVE"
  | "EXPIRED"
  | "CANCELLED"
  | "REJECTED";

export type SubscriptionApiResponse = {
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
  status: SubscriptionStatus;
  subscriptionId: string;
  ticketTypeId: string;
  updatedAt?: string | null;
  updatedBy?: string | null;
};

export type SubscriptionFilter = {
  effectiveFrom?: string;
  effectiveTo?: string;
  keyword?: string;
  status?: SubscriptionStatus;
  ticketTypeId?: string;
};

export type CustomerApiResponse = {
  accountEmail?: string | null;
  approvalStatus?: string | null;
  customerCode?: string | null;
  customerId: string;
  customerType?: string | null;
  status?: string | null;
  userProfile?: {
    fullName?: string | null;
    identifyCard?: string | null;
    phoneNumber?: string | null;
  } | null;
};

export type CustomerVehicleApiResponse = {
  brand?: string | null;
  color?: string | null;
  customerId?: string | null;
  customerVehicleId: string;
  isDefault?: boolean | null;
  licensePlate?: string | null;
  status?: string | null;
  vehicleTypeId?: string | null;
};

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

export async function getSubscriptions(filter: SubscriptionFilter = {}) {
  const response = await apiClient<ApiResponse<SubscriptionApiResponse[]>>(
    `${apiEndpoints.accessControl.subscriptions}${buildQuery(filter)}`,
  );
  return response.data ?? [];
}

export async function approveSubscription(subscriptionId: string) {
  const response = await apiClient<ApiResponse<SubscriptionApiResponse>>(
    `${apiEndpoints.accessControl.subscriptions}/${subscriptionId}/approve`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function rejectSubscription(subscriptionId: string, reason: string) {
  const response = await apiClient<ApiResponse<SubscriptionApiResponse>>(
    `${apiEndpoints.accessControl.subscriptions}/${subscriptionId}/reject`,
    {
      body: { reason },
      method: "PATCH",
    },
  );
  return response.data;
}

export async function assignSubscriptionCard(subscriptionId: string) {
  const response = await apiClient<ApiResponse<SubscriptionApiResponse>>(
    `${apiEndpoints.accessControl.subscriptions}/${subscriptionId}/assign-card`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function getCustomers() {
  const response = await apiClient<ApiResponse<CustomerApiResponse[]>>(apiEndpoints.people.customers);
  return response.data ?? [];
}

export async function getCustomerVehicles() {
  const response = await apiClient<ApiResponse<CustomerVehicleApiResponse[]>>(apiEndpoints.people.customerVehicles);
  return response.data ?? [];
}

export async function getSubscriptionLookupData() {
  const [customers, customerVehicles, ticketTypesResponse, vehicleTypesResponse, cards] = await Promise.all([
    getCustomers(),
    getCustomerVehicles(),
    getTicketTypes(),
    getVehicleTypes(),
    fetchCards(),
  ]);

  return {
    cards,
    customers,
    customerVehicles,
    ticketTypes: ticketTypesResponse.data ?? [],
    vehicleTypes: vehicleTypesResponse.data ?? [],
  };
}

export type { CardResponse, TicketTypeApiResponse, VehicleTypeApiResponse };
