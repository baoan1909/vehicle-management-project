import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type AccountDeletionRequest = {
  requestId: string;
  status: "GRACE_PERIOD" | "CANCELLED" | "PROCESSING" | "RETRY" | "REVIEW_REQUIRED" | "COMPLETED";
  requestedAt: string;
  scheduledDeletionAt: string;
  cancelledAt?: string | null;
  completedAt?: string | null;
  cancellable: boolean;
};

export async function getCurrentAccountDeletionRequest() {
  const response = await apiClient<ApiResponse<AccountDeletionRequest | null>>(
    apiEndpoints.iam.accountProfile.currentDeletionRequest,
  );
  return response.data;
}

export async function requestCurrentAccountDeletion() {
  const response = await apiClient<ApiResponse<AccountDeletionRequest>>(
    apiEndpoints.iam.accountProfile.deletionRequests,
    { method: "POST" },
  );
  return response.data;
}

export async function cancelCurrentAccountDeletion(reason?: string) {
  const response = await apiClient<ApiResponse<AccountDeletionRequest>>(
    apiEndpoints.iam.accountProfile.currentDeletionRequest,
    { method: "DELETE", body: { reason } },
  );
  return response.data;
}
