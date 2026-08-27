import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import type { ChatConversationResponse } from "@/features/support/api/chatApi";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type SupportTicketPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";
export type SupportTicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";
export type SupportTicketCategoryStatus = "ACTIVE" | "INACTIVE";

export type SupportTicketCategoryResponse = {
  categoryId: string;
  code: string;
  createdAt: string | null;
  createdBy: string | null;
  description: string | null;
  name: string;
  priority: SupportTicketPriority;
  status: SupportTicketCategoryStatus;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type SupportTicketResponse = {
  assignedTo: string | null;
  categoryCode: string | null;
  categoryId: string;
  categoryName: string | null;
  closedAt: string | null;
  closedBy: string | null;
  content: string;
  createdAt: string | null;
  createdBy: string | null;
  customerId: string;
  lastReopenedAt: string | null;
  priority: SupportTicketPriority;
  reopenCount: number | null;
  resolvedAt: string | null;
  resolutionNote: string | null;
  status: SupportTicketStatus;
  supportTicketId: string;
  title: string;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type SupportTicketCategoryFilter = {
  keyword?: string;
  priority?: SupportTicketPriority;
  status?: SupportTicketCategoryStatus;
};

export type SupportTicketFilter = {
  assignedTo?: string;
  categoryId?: string;
  customerId?: string;
  keyword?: string;
  priority?: SupportTicketPriority;
  status?: SupportTicketStatus;
};

export type SaveSupportTicketCategoryRequest = {
  code: string;
  description?: string | null;
  name: string;
  priority: SupportTicketPriority;
};

export type SaveSupportTicketRequest = {
  categoryId: string;
  content: string;
  title: string;
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

export function getSupportTicketCategories(filter: SupportTicketCategoryFilter = {}) {
  return apiClient<ApiResponse<SupportTicketCategoryResponse[]>>(
    `${apiEndpoints.operations.supportTicketCategories}${buildQuery(filter)}`,
  );
}

export function createSupportTicketCategory(payload: SaveSupportTicketCategoryRequest) {
  return apiClient<ApiResponse<SupportTicketCategoryResponse>>(apiEndpoints.operations.supportTicketCategories, {
    body: payload,
    method: "POST",
  });
}

export function updateSupportTicketCategory(categoryId: string, payload: SaveSupportTicketCategoryRequest) {
  return apiClient<ApiResponse<SupportTicketCategoryResponse>>(`${apiEndpoints.operations.supportTicketCategories}/${categoryId}`, {
    body: payload,
    method: "PUT",
  });
}

export function deactivateSupportTicketCategory(categoryId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.operations.supportTicketCategories}/${categoryId}`, {
    method: "DELETE",
  });
}

export function activateSupportTicketCategory(categoryId: string) {
  return apiClient<ApiResponse<SupportTicketCategoryResponse>>(`${apiEndpoints.operations.supportTicketCategories}/${categoryId}/activate`, {
    method: "PATCH",
  });
}

export function getSupportTickets(filter: SupportTicketFilter = {}) {
  return apiClient<ApiResponse<SupportTicketResponse[]>>(
    `${apiEndpoints.operations.supportTickets}${buildQuery(filter)}`,
  );
}

export function getSupportTicketById(ticketId: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(`${apiEndpoints.operations.supportTickets}/${ticketId}`);
}

export function createSupportTicket(payload: SaveSupportTicketRequest) {
  return apiClient<ApiResponse<SupportTicketResponse>>(apiEndpoints.operations.supportTickets, {
    body: payload,
    method: "POST",
  });
}

export function createSupportTicketFromConversation(conversationId: string, payload: SaveSupportTicketRequest) {
  return apiClient<ApiResponse<SupportTicketResponse>>(apiEndpoints.operations.supportTicketsFromConversation(conversationId), {
    body: payload,
    method: "POST",
  });
}

export function openSupportTicketCustomerConversation(ticketId: string) {
  return apiClient<ApiResponse<ChatConversationResponse>>(apiEndpoints.operations.supportTicketCustomerConversation(ticketId), {
    method: "POST",
  });
}

export function updateSupportTicket(ticketId: string, payload: SaveSupportTicketRequest) {
  return apiClient<ApiResponse<SupportTicketResponse>>(`${apiEndpoints.operations.supportTickets}/${ticketId}`, {
    body: payload,
    method: "PUT",
  });
}

export function assignSupportTicket(ticketId: string, assignedTo: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(`${apiEndpoints.operations.supportTickets}/${ticketId}/assign`, {
    body: { assignedTo },
    method: "PATCH",
  });
}

export function startSupportTicketProgress(ticketId: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(`${apiEndpoints.operations.supportTickets}/${ticketId}/start-progress`, {
    method: "PATCH",
  });
}

export function resolveSupportTicket(ticketId: string, resolutionNote: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(`${apiEndpoints.operations.supportTickets}/${ticketId}/resolve`, {
    body: { resolutionNote },
    method: "PATCH",
  });
}

export function reopenSupportTicket(ticketId: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(`${apiEndpoints.operations.supportTickets}/${ticketId}/reopen`, {
    method: "PATCH",
  });
}

export function closeSupportTicket(ticketId: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(`${apiEndpoints.operations.supportTickets}/${ticketId}/close`, {
    method: "PATCH",
  });
}
