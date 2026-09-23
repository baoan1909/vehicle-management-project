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
export type ApprovalRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type SupportTicketEscalationReason = "RESPONSE_DELAY" | "UNRESOLVED" | "INAPPROPRIATE_COMMUNICATION" | "REQUEST_DIFFERENT_ASSIGNEE" | "OTHER";
export type SupportTicketEscalationDecision = "KEEP_ASSIGNEE" | "REASSIGN";

export type SupportTicketEscalationResponse = {
  escalationId: string;
  supportTicketId: string;
  status: ApprovalRequestStatus;
  reasonCode: SupportTicketEscalationReason;
  description: string;
  requestedBy: string;
  currentAssigneeId: string | null;
  requestedAt: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  decision: SupportTicketEscalationDecision | null;
  reassignedTo: string | null;
  decisionNote: string | null;
};

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
  source: "ASSISTANT_CHAT" | "CUSTOMER_PORTAL" | "EMPLOYEE_CHAT" | "OTHER";
  sourceConversationId: string | null;
  sourceMessageId: string | null;
  firstRespondedAt: string | null;
};

export type SupportTicketChatIntakeResponse = {
  ticket: SupportTicketResponse;
  conversation: ChatConversationResponse;
  reusedActiveTicket: boolean;
};

export type AssistantStatusResponse = {
  enabled: boolean;
};

export type AssistantMessageStatusResponse = {
  inputMessageId: string;
  status: "QUEUED" | "PENDING" | "PROCESSING" | "WAITING_CONFIRMATION" | "RETRYING" | "COMPLETED" | "FAILED" | "DISABLED" | "EXPIRED";
  errorCode: string | null;
  terminal: boolean;
};

export type AiToolCallStatus = "REQUESTED" | "VALIDATED" | "AWAITING_CONFIRMATION" | "EXECUTING" | "SUCCEEDED" | "FAILED" | "DENIED" | "EXPIRED";

export type AiToolCallResponse = {
  toolCallId: string;
  conversationId: string | null;
  inputMessageId: string | null;
  toolName: string;
  toolType: "READ_ONLY" | "WRITE";
  status: AiToolCallStatus;
  requestPayloadRedacted: string;
  responsePayloadRedacted: string;
  expiresAt: string | null;
  failureCode: string | null;
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

export function createSupportTicket(payload: SaveSupportTicketRequest, idempotencyKey?: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(apiEndpoints.operations.supportTickets, {
    body: payload,
    headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
    method: "POST",
  });
}

export function createSupportTicketChatIntake(payload: SaveSupportTicketRequest, idempotencyKey: string) {
  return apiClient<ApiResponse<SupportTicketChatIntakeResponse>>(`${apiEndpoints.operations.supportTickets}/chat-intake`, {
    body: payload,
    headers: { "Idempotency-Key": idempotencyKey },
    method: "POST",
  });
}

export function getSupportAssistantConversation() {
  return apiClient<ApiResponse<ChatConversationResponse>>(apiEndpoints.operations.supportAssistantConversation);
}

export function getAssistantStatus() {
  return apiClient<ApiResponse<AssistantStatusResponse>>(apiEndpoints.ai.assistantStatus);
}

export function getAssistantMessageStatus(inputMessageId: string) {
  return apiClient<ApiResponse<AssistantMessageStatusResponse>>(apiEndpoints.ai.assistantMessageStatus(inputMessageId));
}

export type MessageCitationResponse = {
  citationId: string;
  messageId: string;
  documentId: string;
  chunkId: string;
  label: string | null;
  title: string;
  sourcePage: number | null;
  sourceSection: string | null;
  retrievalScore: number | null;
  retrievalAuditId: string | null;
  indexVersionId: string | null;
  citationOrder: number;
};

export type MessageCitationsResponse = {
  messageId: string;
  citations: MessageCitationResponse[];
  groundedConfidence: number | null;
  handoffRecommended: boolean;
  diagnosticCode: string | null;
};

export function getMessageCitations(messageId: string) {
  return apiClient<ApiResponse<MessageCitationsResponse>>(apiEndpoints.ai.messageCitations(messageId));
}

export function getAiToolCall(toolCallId: string) {
  return apiClient<ApiResponse<AiToolCallResponse>>(apiEndpoints.ai.toolCall(toolCallId));
}

export function confirmAiToolCall(toolCallId: string) {
  return apiClient<ApiResponse<AiToolCallResponse>>(apiEndpoints.ai.confirmToolCall(toolCallId), { method: "POST" });
}

export function denyAiToolCall(toolCallId: string) {
  return apiClient<ApiResponse<AiToolCallResponse>>(apiEndpoints.ai.denyToolCall(toolCallId), { method: "POST" });
}

export function createSupportTicketFromConversation(conversationId: string, payload: SaveSupportTicketRequest, idempotencyKey?: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(apiEndpoints.operations.supportTicketsFromConversation(conversationId), {
    body: payload,
    headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
    method: "POST",
  });
}

export function getMySupportTickets(filter: Pick<SupportTicketFilter, "keyword" | "status"> = {}) {
  return apiClient<ApiResponse<SupportTicketResponse[]>>(
    `${apiEndpoints.operations.supportTickets}/mine${buildQuery(filter)}`,
  );
}

export function getConversationSupportTicketHistory(
  conversationId: string,
  filter: Pick<SupportTicketFilter, "keyword" | "status"> = {},
) {
  return apiClient<ApiResponse<SupportTicketResponse[]>>(
    `${apiEndpoints.operations.supportTickets}/conversations/${conversationId}/history${buildQuery(filter)}`,
  );
}

export function shareSupportTicketWithAssistant(ticketId: string) {
  return apiClient<ApiResponse<SupportTicketResponse>>(
    `${apiEndpoints.operations.supportTickets}/assistant-conversation/tickets/${ticketId}`,
    { method: "POST" },
  );
}

export function openSupportTicketCustomerConversation(ticketId: string) {
  return apiClient<ApiResponse<ChatConversationResponse>>(apiEndpoints.operations.supportTicketCustomerConversation(ticketId), {
    method: "POST",
  });
}

export function getActiveSupportTicketCustomerConversation(ticketId: string) {
  return apiClient<ApiResponse<ChatConversationResponse>>(apiEndpoints.operations.supportTicketCustomerConversation(ticketId));
}

export function createSupportTicketEscalation(
  ticketId: string,
  payload: { reasonCode: SupportTicketEscalationReason; description: string },
  idempotencyKey: string,
) {
  return apiClient<ApiResponse<SupportTicketEscalationResponse>>(
    apiEndpoints.operations.supportTicketEscalationForTicket(ticketId),
    { body: payload, headers: { "Idempotency-Key": idempotencyKey }, method: "POST" },
  );
}

export function getMyCurrentSupportTicketEscalation(ticketId: string) {
  return apiClient<ApiResponse<SupportTicketEscalationResponse | null>>(
    `${apiEndpoints.operations.supportTicketEscalationForTicket(ticketId)}/mine/current`,
  );
}

export function getSupportTicketEscalations(status?: ApprovalRequestStatus) {
  return apiClient<ApiResponse<SupportTicketEscalationResponse[]>>(
    `${apiEndpoints.operations.supportTicketEscalations}${buildQuery({ status })}`,
  );
}

export function approveSupportTicketEscalation(
  escalationId: string,
  payload: { decision: SupportTicketEscalationDecision; assignedTo?: string; note: string },
) {
  return apiClient<ApiResponse<SupportTicketEscalationResponse>>(
    `${apiEndpoints.operations.supportTicketEscalations}/${escalationId}/approve`,
    { body: payload, method: "PATCH" },
  );
}

export function rejectSupportTicketEscalation(escalationId: string, note: string) {
  return apiClient<ApiResponse<SupportTicketEscalationResponse>>(
    `${apiEndpoints.operations.supportTicketEscalations}/${escalationId}/reject`,
    { body: { note }, method: "PATCH" },
  );
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
