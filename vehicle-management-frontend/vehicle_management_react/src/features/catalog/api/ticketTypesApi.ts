import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type TicketTypeStatus = "ACTIVE" | "INACTIVE";

export type TicketTypeFilter = {
  keyword?: string;
  status?: TicketTypeStatus;
};

export type CreateTicketTypeRequest = {
  code: string;
  description?: string | null;
  name: string;
};

export type UpdateTicketTypeRequest = CreateTicketTypeRequest;

export type TicketTypeApiResponse = {
  code: string;
  createdAt: string | null;
  createdBy: string | null;
  description: string | null;
  durationDays: number | null;
  name: string;
  status: TicketTypeStatus | null;
  ticketTypeId: string;
  updatedAt: string | null;
  updatedBy: string | null;
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

export function getTicketTypes(filter: TicketTypeFilter = {}) {
  return apiClient<ApiResponse<TicketTypeApiResponse[]>>(
    `${apiEndpoints.catalog.ticketTypes}${buildQuery(filter)}`,
  );
}

export function createTicketType(payload: CreateTicketTypeRequest) {
  return apiClient<ApiResponse<TicketTypeApiResponse>>(apiEndpoints.catalog.ticketTypes, {
    method: "POST",
    body: payload,
  });
}

export function updateTicketType(ticketTypeId: string, payload: UpdateTicketTypeRequest) {
  return apiClient<ApiResponse<TicketTypeApiResponse>>(`${apiEndpoints.catalog.ticketTypes}/${ticketTypeId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivateTicketType(ticketTypeId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.catalog.ticketTypes}/${ticketTypeId}`, {
    method: "DELETE",
  });
}

export function activateTicketType(ticketTypeId: string) {
  return apiClient<ApiResponse<TicketTypeApiResponse>>(`${apiEndpoints.catalog.ticketTypes}/${ticketTypeId}/activate`, {
    method: "PATCH",
  });
}
