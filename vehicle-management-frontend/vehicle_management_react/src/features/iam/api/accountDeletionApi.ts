import { apiClient } from "@/core/api/apiClient";
import { isApiError } from "@/core/api/apiError";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type AccountDeletionRequest = {
  requestId: string;
  status: "GRACE_PERIOD" | "CANCELLED" | "PROCESSING" | "RETRY" | "BLOCKED" | "REVIEW_REQUIRED" | "COMPLETED";
  requestedAt: string;
  scheduledDeletionAt: string;
  cancelledAt?: string | null;
  completedAt?: string | null;
  cancellable: boolean;
};

export type AccountDeletionBlocker = {
  code: string;
  message: string;
  resourceType: string;
  resourceId?: string | null;
  organizationId?: string | null;
  parkingLotId?: string | null;
  remediationAction?: string | null;
  remediationUrl?: string | null;
};

export type AccountDeletionAutomaticAction = {
  code: string;
  message: string;
  resourceType: string;
  resourceId?: string | null;
  action: string;
  scheduledAt?: string | null;
  estimatedRefundAmount?: number | null;
  remediationUrl?: string | null;
};

export function getAccountDeletionRemediationPath(blocker: AccountDeletionBlocker) {
  const path = blocker.remediationUrl?.trim();
  return path?.startsWith("/") && !path.startsWith("//") ? path : null;
}

export type OrganizationClosureImpact = {
  organizationId: string;
  organizationCode: string;
  organizationName: string;
  affectedParkingLotCount: number;
  affectedMemberCount: number;
};

export type AccountDeletionImpact = {
  accountBlockers: AccountDeletionBlocker[];
  organizationBlockers: AccountDeletionBlocker[];
  automaticActions: AccountDeletionAutomaticAction[];
  organizationClosureScopes: OrganizationClosureImpact[];
  affectedOrganizationCount: number;
  affectedParkingLotCount: number;
  affectedMemberCount: number;
  dataToArchive: string[];
  dataToRetain: string[];
  confirmationRequired: boolean;
};

export function getAccountDeletionBlockers(error: unknown): AccountDeletionBlocker[] {
  if (!isApiError(error) || error.code !== "ACCOUNT_DELETION_BLOCKED") return [];
  if (!error.data || typeof error.data !== "object" || !("blockers" in error.data)) return [];
  const blockers = error.data.blockers;
  if (!Array.isArray(blockers)) return [];

  return blockers.filter((blocker): blocker is AccountDeletionBlocker => Boolean(
    blocker
    && typeof blocker === "object"
    && "code" in blocker
    && typeof blocker.code === "string"
    && "message" in blocker
    && typeof blocker.message === "string"
    && "resourceType" in blocker
    && typeof blocker.resourceType === "string",
  ));
}

export async function getCurrentAccountDeletionRequest() {
  const response = await apiClient<ApiResponse<AccountDeletionRequest | null>>(
    apiEndpoints.iam.accountProfile.currentDeletionRequest,
  );
  return response.data;
}

export async function getCurrentAccountDeletionImpact() {
  const response = await apiClient<ApiResponse<AccountDeletionImpact>>(
    apiEndpoints.iam.accountProfile.deletionImpact,
  );
  return response.data;
}

export async function requestCurrentAccountDeletion(
  confirmOrganizationClosure = false,
  confirmActiveSubscriptionTerminationWithoutRefund = false,
) {
  const response = await apiClient<ApiResponse<AccountDeletionRequest>>(
    apiEndpoints.iam.accountProfile.deletionRequests,
    { method: "POST", body: {
      confirmOrganizationClosure,
      confirmActiveSubscriptionTerminationWithoutRefund,
    } },
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
