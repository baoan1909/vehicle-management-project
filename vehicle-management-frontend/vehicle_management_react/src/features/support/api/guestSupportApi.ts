import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type PublicSupportCategory = {
  categoryId: string;
  code: string;
  name: string;
  description: string | null;
  priority: "LOW" | "NORMAL" | "HIGH" | "URGENT";
};

export type GuestSupportRequest = {
  referenceCode: string;
  categoryId: string;
  title: string;
  content: string;
  maskedContact: string;
  status: "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";
  createdAt: string;
  capabilityExpiresAt: string;
  capabilityToken: string | null;
};

export type GuestAssistantAnswer = {
  answer: string;
  citations: Array<{ label: string; title: string; page: number | null; section: string | null }>;
  handoffRecommended: boolean;
  diagnosticCode: string | null;
};

export function getPublicSupportCategories() {
  return apiClient<ApiResponse<PublicSupportCategory[]>>(apiEndpoints.public.support.categories, { skipAuth: true });
}

export function getGuestAssistantStatus() {
  return apiClient<ApiResponse<{ enabled: boolean }>>(apiEndpoints.public.support.assistantStatus, { skipAuth: true });
}

export function askGuestAssistant(message: string, sessionId: string) {
  return apiClient<ApiResponse<GuestAssistantAnswer>>(apiEndpoints.public.support.assistantMessages, {
    body: { message, sessionId },
    method: "POST",
    skipAuth: true,
    timeoutMs: 30_000,
  });
}

export function createGuestSupportRequest(
  payload: { categoryId: string; title: string; content: string; email?: string; phone?: string; sessionId: string },
  idempotencyKey: string,
) {
  return apiClient<ApiResponse<GuestSupportRequest>>(apiEndpoints.public.support.requests, {
    body: payload,
    headers: { "Idempotency-Key": idempotencyKey },
    method: "POST",
    skipAuth: true,
  });
}

export function accessGuestSupportRequest(referenceCode: string, capabilityToken: string) {
  return apiClient<ApiResponse<GuestSupportRequest>>(apiEndpoints.public.support.requestAccess, {
    body: { referenceCode, capabilityToken },
    method: "POST",
    skipAuth: true,
  });
}
