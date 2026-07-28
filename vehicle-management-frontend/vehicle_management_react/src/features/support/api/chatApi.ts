import { appConfig } from "@/config/env";
import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { getValidAccessToken, refreshAccessToken } from "@/core/auth/tokenRefresh";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type ChatConversationType =
  | "INTERNAL_DIRECT"
  | "INTERNAL_GROUP"
  | "CUSTOMER_DIRECT"
  | "SUPPORT_TICKET"
  | "PARKING_SESSION"
  | "BILLING"
  | "LOST_CARD"
  | "SYSTEM_DIRECT";

export type ChatConversationStatus = "ACTIVE" | "ARCHIVED" | "CLOSED";
export type ChatMessageType = "TEXT" | "IMAGE" | "FILE" | "SYSTEM" | "CONTEXT_CARD" | "ACTION_CARD" | "SUPPORT_REQUEST";
export type ChatAttachmentType = "IMAGE" | "DOCUMENT" | "AUDIO" | "PARKING_EVIDENCE" | "PAYMENT_PROOF";
export type ChatMemberRole = "OWNER" | "MEMBER" | "ASSIGNEE" | "OBSERVER" | "CUSTOMER";

export type ChatConversationParticipantResponse = {
  accountId: string;
  avatarUrl: string | null;
  email: string | null;
  fullName: string | null;
  memberRole: ChatMemberRole;
  username: string | null;
};

export type ChatConversationResponse = {
  assignedTo: string | null;
  conversationId: string;
  conversationType: ChatConversationType;
  customerId: string | null;
  lastMessageAt: string | null;
  lastMessageId: string | null;
  ownerAccountId: string | null;
  participants: ChatConversationParticipantResponse[];
  relatedId: string | null;
  relatedSchema: string | null;
  relatedTable: string | null;
  status: ChatConversationStatus;
  supportTicketId: string | null;
  title: string | null;
};

export type ChatAttachmentResponse = {
  attachmentId: string;
  attachmentType: ChatAttachmentType;
  contentType: string | null;
  height: number | null;
  messageId: string;
  originalFilename: string | null;
  sizeBytes: number | null;
  width: number | null;
};

export type ChatMessageResponse = {
  attachments: ChatAttachmentResponse[];
  content: string | null;
  conversationId: string;
  createdAt: string | null;
  deleted: boolean;
  deletedAt: string | null;
  editedAt: string | null;
  messageId: string;
  messageType: ChatMessageType;
  relatedId: string | null;
  relatedSchema: string | null;
  relatedTable: string | null;
  replyToMessageId: string | null;
  senderAccountId: string | null;
};

export type ChatInboxItemResponse = {
  conversation: ChatConversationResponse;
  lastMessage: ChatMessageResponse | null;
  unreadCount: number;
};

export type ChatAttachmentReadUrlResponse = {
  attachmentId: string;
  expireSeconds: number;
  readUrl: string;
};

export type ChatRealtimeEvent = {
  conversationId: string;
  message: ChatMessageResponse | null;
  messageId: string;
  occurredAt: string | null;
};

export type CreateInternalDirectConversationRequest = {
  targetAccountId: string;
};

export type CreateCustomerSupportConversationRequest = {
  customerId: string;
  title?: string | null;
};

export function getChatInbox() {
  return apiClient<ApiResponse<ChatInboxItemResponse[]>>(`${apiEndpoints.operations.chat}/conversations`);
}

export function getChatConversation(conversationId: string) {
  return apiClient<ApiResponse<ChatConversationResponse>>(`${apiEndpoints.operations.chat}/conversations/${conversationId}`);
}

export function createInternalDirectConversation(targetAccountId: string) {
  return apiClient<ApiResponse<ChatConversationResponse>>(`${apiEndpoints.operations.chat}/conversations/internal/direct`, {
    body: { targetAccountId } satisfies CreateInternalDirectConversationRequest,
    method: "POST",
  });
}

export function createCustomerSupportConversation(payload: CreateCustomerSupportConversationRequest) {
  return apiClient<ApiResponse<ChatConversationResponse>>(`${apiEndpoints.operations.chat}/conversations/customer-support`, {
    body: payload,
    method: "POST",
  });
}

export function getChatMessages(conversationId: string, options: { beforeCreatedAt?: string; limit?: number } = {}) {
  const params = new URLSearchParams();
  if (options.beforeCreatedAt) params.set("beforeCreatedAt", options.beforeCreatedAt);
  if (options.limit) params.set("limit", String(options.limit));
  const query = params.toString();

  return apiClient<ApiResponse<ChatMessageResponse[]>>(
    `${apiEndpoints.operations.chat}/conversations/${conversationId}/messages${query ? `?${query}` : ""}`,
  );
}

export function sendChatTextMessage(conversationId: string, content: string, replyToMessageId?: string | null) {
  return apiClient<ApiResponse<ChatMessageResponse>>(`${apiEndpoints.operations.chat}/conversations/${conversationId}/messages`, {
    body: { content, replyToMessageId: replyToMessageId ?? null },
    method: "POST",
  });
}

export async function sendChatImageMessage(conversationId: string, content: string | null, files: File[]) {
  const formData = new FormData();
  files.forEach((file) => formData.append("files", file, file.name));
  if (content?.trim()) {
    formData.append("content", content.trim());
  }

  return postMultipart<ApiResponse<ChatMessageResponse>>(
    `${apiEndpoints.operations.chat}/conversations/${conversationId}/attachments`,
    formData,
  );
}

export function markChatConversationRead(conversationId: string, messageId?: string | null) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.operations.chat}/conversations/${conversationId}/read`, {
    body: messageId ? { messageId } : null,
    method: "POST",
  });
}

export function deleteChatMessage(messageId: string) {
  return apiClient<ApiResponse<void>>(`${apiEndpoints.operations.chat}/messages/${messageId}`, {
    method: "DELETE",
  });
}

export function getChatAttachmentReadUrl(attachmentId: string) {
  return apiClient<ApiResponse<ChatAttachmentReadUrlResponse>>(`${apiEndpoints.operations.chat}/attachments/${attachmentId}/read-url`);
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
