import { apiEndpoints } from "@/core/api/apiEndpoints";
import { apiClient } from "@/core/api/apiClient";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type CardStatus = "AVAILABLE" | "ASSIGNED" | "IN_USE" | "RESERVED" | "BLOCKED" | "LOST" | "RETIRED";

export type CardResponse = {
  blockedAt?: string | null;
  blockedBy?: string | null;
  blockedReason?: string | null;
  cardId: string;
  cardNumber: string;
  cardTypeId?: string | null;
  cardReceiptDate?: string | null;
  createdAt?: string | null;
  customerApprovalStatus?: string | null;
  customerCode?: string | null;
  customerEmail?: string | null;
  customerFullName?: string | null;
  customerId?: string | null;
  customerPhoneNumber?: string | null;
  customerStatus?: string | null;
  customerType?: string | null;
  customerVehicleId?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  issuedAt?: string | null;
  licensePlate?: string | null;
  registeredVehicleTypeCode?: string | null;
  registeredVehicleTypeId?: string | null;
  registeredVehicleTypeName?: string | null;
  requestedEffectiveFrom?: string | null;
  status: CardStatus;
  statusBeforeBlocked?: CardStatus | null;
  subscriptionId?: string | null;
  subscriptionPrice?: number | null;
  subscriptionStatus?: string | null;
  ticketTypeCode?: string | null;
  ticketTypeId?: string | null;
  ticketTypeName?: string | null;
  uid: string;
  updatedAt?: string | null;
  vehicleBrand?: string | null;
  vehicleColor?: string | null;
};

export type CardTypeResponse = {
  cardTypeId: string;
  code: string;
  description?: string | null;
  isActive?: boolean | null;
  isReturnRequired?: boolean | null;
  name: string;
};

export type CardFilters = {
  cardTypeId?: string;
  keyword?: string;
  status?: CardStatus;
};

export type CardPayload = {
  cardNumber: string;
  cardTypeId: string;
  uid: string;
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

export async function fetchCards(filters: CardFilters = {}) {
  const query = toQueryString({
    cardTypeId: filters.cardTypeId,
    keyword: filters.keyword,
    status: filters.status,
  });
  const response = await apiClient<ApiResponse<CardResponse[]>>(`${apiEndpoints.cards.cards}${query}`);
  return response.data ?? [];
}

export async function fetchCardTypes() {
  const response = await apiClient<ApiResponse<CardTypeResponse[]>>(`${apiEndpoints.catalog.cardTypes}?isActive=true`);
  return response.data ?? [];
}

export async function createCard(payload: CardPayload) {
  const response = await apiClient<ApiResponse<CardResponse>>(apiEndpoints.cards.cards, {
    body: payload,
    method: "POST",
  });
  return response.data;
}

export async function updateCard(cardId: string, payload: CardPayload) {
  const response = await apiClient<ApiResponse<CardResponse>>(`${apiEndpoints.cards.cards}/${cardId}`, {
    body: payload,
    method: "PUT",
  });
  return response.data;
}

export async function blockCard(cardId: string, reason: string) {
  const response = await apiClient<ApiResponse<CardResponse>>(`${apiEndpoints.cards.cards}/${cardId}/block`, {
    body: { reason },
    method: "PATCH",
  });
  return response.data;
}

export async function unblockCard(cardId: string) {
  const response = await apiClient<ApiResponse<CardResponse>>(`${apiEndpoints.cards.cards}/${cardId}/unblock`, {
    method: "PATCH",
  });
  return response.data;
}

export async function retireCard(cardId: string, reason: string) {
  const response = await apiClient<ApiResponse<CardResponse>>(`${apiEndpoints.cards.cards}/${cardId}/retire`, {
    body: { reason },
    method: "PATCH",
  });
  return response.data;
}
