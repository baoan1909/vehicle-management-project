import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type CustomerStatus = "ACTIVE" | "INACTIVE";
export type CustomerApprovalStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";
export type CustomerType = "REGISTERED" | "VIP";
export type CustomerVehicleStatus = "ACTIVE" | "INACTIVE" | "BLOCKED";

export type UserProfileAdminResponse = {
  address?: string | null;
  avatarUrl?: string | null;
  createdAt?: string | null;
  dateOfBirth?: string | null;
  fullName?: string | null;
  gender?: string | null;
  identifyCard?: string | null;
  phoneNumber?: string | null;
  status?: string | null;
  updatedAt?: string | null;
  userProfileId: string;
};

export type CustomerAdminResponse = {
  accountEmail?: string | null;
  approvalStatus: CustomerApprovalStatus;
  approvedAt?: string | null;
  approvedBy?: string | null;
  createdAt?: string | null;
  customerCode?: string | null;
  customerId: string;
  customerType: CustomerType;
  status: CustomerStatus;
  updatedAt?: string | null;
  userProfile?: UserProfileAdminResponse | null;
  userProfileId?: string | null;
};

export type CustomerVehicleAdminResponse = {
  brand?: string | null;
  color?: string | null;
  createdAt?: string | null;
  customerId: string;
  customerVehicleId: string;
  isDefault?: boolean | null;
  licensePlate: string;
  status: CustomerVehicleStatus;
  updatedAt?: string | null;
  vehicleTypeId?: string | null;
};

export type CustomerSubscriptionCardResponse = {
  cardId: string;
  cardNumber: string;
  cardTypeId?: string | null;
  customerCode?: string | null;
  customerId?: string | null;
  customerVehicleId?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  licensePlate?: string | null;
  status?: string | null;
  subscriptionId?: string | null;
  subscriptionPrice?: number | null;
  subscriptionStatus?: string | null;
  ticketTypeCode?: string | null;
  ticketTypeId?: string | null;
  ticketTypeName?: string | null;
  uid?: string | null;
  vehicleBrand?: string | null;
  vehicleColor?: string | null;
};

export type CustomerAdminProfileResponse = {
  customer: CustomerAdminResponse;
  customerVehicles?: CustomerVehicleAdminResponse[] | null;
  userProfile?: UserProfileAdminResponse | null;
};

export type CustomerOnboardingApprovalResponse = {
  account?: {
    accountId?: string | null;
    accountStatus?: string | null;
    email?: string | null;
    roleCode?: string | null;
    username?: string | null;
  } | null;
  customer?: {
    approvedAt?: string | null;
    approvedBy?: string | null;
    customerApprovalStatus?: string | null;
    customerCode?: string | null;
    customerId?: string | null;
    customerStatus?: string | null;
    customerType?: string | null;
  } | null;
  profile?: {
    fullName?: string | null;
    phoneNumber?: string | null;
    userProfileId?: string | null;
  } | null;
  request?: {
    approvalRequestId: string;
    approvalRequestStatus?: string | null;
    approvedAt?: string | null;
    approvedBy?: string | null;
    createdAt?: string | null;
    note?: string | null;
    requestedBy?: string | null;
    requestType?: string | null;
    updatedAt?: string | null;
  } | null;
};

export type VehicleTypeResponse = {
  code: string;
  description?: string | null;
  isActive?: boolean | null;
  name: string;
  vehicleTypeId: string;
};

export type CustomerFilters = {
  approvalStatus?: CustomerApprovalStatus;
  customerType?: CustomerType;
  keyword?: string;
  status?: CustomerStatus;
};

export type UpdateCustomerAdminProfilePayload = {
  customer: {
    customerType: CustomerType;
  };
  userProfile: {
    address?: string | null;
    dateOfBirth?: string | null;
    fullName?: string | null;
    gender?: string | null;
    identifyCard?: string | null;
    phoneNumber?: string | null;
    status?: string | null;
  };
};

export type CustomerVehiclePayload = {
  brand?: string | null;
  color?: string | null;
  isDefault?: boolean | null;
  licensePlate: string;
  vehicleTypeId: string;
};

function toQueryString(params: Record<string, string | undefined>) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value?.trim()) {
      searchParams.set(key, value.trim());
    }
  });
  const query = searchParams.toString();
  return query ? `?${query}` : "";
}

export async function fetchCustomers(filters: CustomerFilters = {}) {
  const query = toQueryString({
    approvalStatus: filters.approvalStatus,
    customerType: filters.customerType,
    keyword: filters.keyword,
    status: filters.status,
  });
  const response = await apiClient<ApiResponse<CustomerAdminResponse[]>>(
    `${apiEndpoints.customers.customers}${query}`,
  );
  return response.data ?? [];
}

export async function fetchCustomerById(customerId: string) {
  const response = await apiClient<ApiResponse<CustomerAdminResponse>>(
    `${apiEndpoints.customers.customers}/${customerId}`,
  );
  return response.data;
}

export async function updateCustomerAdminProfile(customerId: string, payload: UpdateCustomerAdminProfilePayload) {
  const response = await apiClient<ApiResponse<CustomerAdminProfileResponse>>(
    `${apiEndpoints.customers.customers}/${customerId}`,
    {
      body: payload,
      method: "PUT",
    },
  );
  return response.data;
}

export async function activateCustomer(customerId: string) {
  const response = await apiClient<ApiResponse<CustomerAdminResponse>>(
    `${apiEndpoints.customers.customers}/${customerId}/activate`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function fetchCustomerOnboardingApprovals(filters: { keyword?: string; status?: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED" } = {}) {
  const query = toQueryString({
    keyword: filters.keyword,
    status: filters.status,
  });
  const response = await apiClient<ApiResponse<CustomerOnboardingApprovalResponse[]>>(
    `${apiEndpoints.operations.customerOnboardingApprovals}${query}`,
  );
  return response.data ?? [];
}

export async function approveCustomerOnboardingApproval(approvalRequestId: string, note?: string) {
  const response = await apiClient<ApiResponse<CustomerOnboardingApprovalResponse>>(
    `${apiEndpoints.operations.customerOnboardingApprovals}/${approvalRequestId}/approve`,
    {
      body: { note: note?.trim() || null },
      method: "PATCH",
    },
  );
  return response.data;
}

export async function inactivateCustomer(customerId: string) {
  const response = await apiClient<ApiResponse<CustomerAdminResponse>>(
    `${apiEndpoints.customers.customers}/${customerId}/inactivate`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function fetchCustomerVehicles(customerId: string) {
  const query = toQueryString({ customerId });
  const response = await apiClient<ApiResponse<CustomerVehicleAdminResponse[]>>(
    `${apiEndpoints.customers.customerVehicles}${query}`,
  );
  return response.data ?? [];
}

export async function fetchCustomerSubscriptionCards(customerId: string, customerCode?: string | null) {
  const query = toQueryString({ keyword: customerCode || customerId });
  const response = await apiClient<ApiResponse<CustomerSubscriptionCardResponse[]>>(
    `${apiEndpoints.accessControl.cards}${query}`,
  );
  return (response.data ?? []).filter((card) => card.customerId === customerId && card.subscriptionId);
}

export async function createCustomerVehicle(customerId: string, payload: CustomerVehiclePayload) {
  const response = await apiClient<ApiResponse<CustomerVehicleAdminResponse[]>>(
    apiEndpoints.customers.customerVehicles,
    {
      body: {
        create: [{ ...payload, customerId }],
        customerId,
        inactivate: [],
        update: [],
      },
      method: "POST",
    },
  );
  return response.data ?? [];
}

export async function updateCustomerVehicle(customerVehicleId: string, payload: CustomerVehiclePayload) {
  const response = await apiClient<ApiResponse<CustomerVehicleAdminResponse>>(
    `${apiEndpoints.customers.customerVehicles}/${customerVehicleId}`,
    {
      body: payload,
      method: "PUT",
    },
  );
  return response.data;
}

export async function deleteCustomerVehicle(customerVehicleId: string) {
  await apiClient<ApiResponse<void>>(`${apiEndpoints.customers.customerVehicles}/${customerVehicleId}`, {
    method: "DELETE",
  });
}

export async function inactivateCustomerVehicle(customerVehicleId: string) {
  const response = await apiClient<ApiResponse<CustomerVehicleAdminResponse>>(
    `${apiEndpoints.customers.customerVehicles}/${customerVehicleId}/inactivate`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function activateCustomerVehicle(customerVehicleId: string) {
  const response = await apiClient<ApiResponse<CustomerVehicleAdminResponse>>(
    `${apiEndpoints.customers.customerVehicles}/${customerVehicleId}/activate`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function markCustomerVehicleAsDefault(customerVehicleId: string) {
  const response = await apiClient<ApiResponse<CustomerVehicleAdminResponse>>(
    `${apiEndpoints.customers.customerVehicles}/${customerVehicleId}/mark-default`,
    { method: "PATCH" },
  );
  return response.data;
}

export async function fetchVehicleTypes() {
  const response = await apiClient<ApiResponse<VehicleTypeResponse[]>>(
    `${apiEndpoints.catalog.vehicleTypes}?isActive=true`,
  );
  return response.data ?? [];
}
