import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = { data: T; message: string; success: boolean; timestamp: string };

export type DeletedAccountArchive = {
  accountId: string;
  accountStatus: "DELETED";
  requestId: string;
  requestStatus: string;
  requestedAt: string;
  scheduledDeletionAt: string;
  completedAt?: string | null;
};

export type ClosedOrganizationArchive = {
  organizationId: string;
  code: string;
  name: string;
  status: "CLOSED";
  closedAt?: string | null;
  closedBy?: string | null;
  closureReason?: string | null;
  closureRequestId?: string | null;
};

export type DeletionCleanupArchive = {
  cleanupItemId: string;
  organizationId?: string | null;
  resourceType: string;
  status: string;
  scheduledAt: string;
  attemptCount: number;
  nextRetryAt?: string | null;
  completedAt?: string | null;
};

export type AccountDeletionAudit = {
  requestId: string;
  accountId: string;
  status: string;
  requestedAt: string;
  scheduledDeletionAt: string;
  cancelledAt?: string | null;
  completedAt?: string | null;
  attemptCount: number;
  lastErrorCode?: string | null;
  cleanupItems: DeletionCleanupArchive[];
};

export type DeletedIdentityType =
  | "USERNAME" | "EMAIL" | "PHONE_NUMBER" | "IDENTIFY_CARD"
  | "CUSTOMER_CODE" | "EMPLOYEE_CODE" | "PROVIDER_SUBJECT";

export type DeletedIdentityMatch = {
  accountId: string;
  requestId: string;
  identityType: DeletedIdentityType;
  capturedAt: string;
};

const root = apiEndpoints.iam.deletionAudit;
const reasonQuery = (reason: string) => encodeURIComponent(reason.trim());

export async function getDeletedAccounts(reason: string, limit = 50) {
  const response = await apiClient<ApiResponse<DeletedAccountArchive[]>>(
    `${root}/accounts?limit=${limit}&reason=${reasonQuery(reason)}`,
  );
  return response.data;
}

export async function getDeletedAccount(accountId: string, reason: string) {
  const response = await apiClient<ApiResponse<DeletedAccountArchive>>(
    `${root}/accounts/${encodeURIComponent(accountId)}?reason=${reasonQuery(reason)}`,
  );
  return response.data;
}

export async function getClosedOrganizations(reason: string, limit = 50) {
  const response = await apiClient<ApiResponse<ClosedOrganizationArchive[]>>(
    `${root}/organizations?limit=${limit}&reason=${reasonQuery(reason)}`,
  );
  return response.data;
}

export async function getClosedOrganization(organizationId: string, reason: string) {
  const response = await apiClient<ApiResponse<ClosedOrganizationArchive>>(
    `${root}/organizations/${encodeURIComponent(organizationId)}?reason=${reasonQuery(reason)}`,
  );
  return response.data;
}

export async function getAccountDeletionAudit(requestId: string, reason: string) {
  const response = await apiClient<ApiResponse<AccountDeletionAudit>>(
    `${root}/requests/${encodeURIComponent(requestId)}?reason=${reasonQuery(reason)}`,
  );
  return response.data;
}

export async function lookupDeletedIdentity(
  identityType: DeletedIdentityType,
  value: string,
  reason: string,
) {
  const response = await apiClient<ApiResponse<DeletedIdentityMatch[]>>(
    `${root}/identity-lookup`,
    { method: "POST", body: { identityType, value: value.trim(), reason: reason.trim() } },
  );
  return response.data;
}
