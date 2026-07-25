import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type OnboardingApprovalKind = "system-admin" | "internal-employee" | "customer";
export type OnboardingApprovalStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

type ApprovalRequestInfo = {
  approvalRequestId: string;
  approvalRequestStatus?: OnboardingApprovalStatus | string | null;
  approvedAt?: string | null;
  approvedBy?: string | null;
  createdAt?: string | null;
  note?: string | null;
  requestedBy?: string | null;
  requestType?: string | null;
  updatedAt?: string | null;
};

type ApprovalAccountInfo = {
  accountId?: string | null;
  accountStatus?: string | null;
  email?: string | null;
  roleCode?: string | null;
  username?: string | null;
};

type ApprovalProfileInfo = {
  address?: string | null;
  dateOfBirth?: string | null;
  fullName?: string | null;
  gender?: string | null;
  identifyCard?: string | null;
  phoneNumber?: string | null;
  userProfileId?: string | null;
};

export type OnboardingApprovalResponse = {
  account?: ApprovalAccountInfo | null;
  customer?: {
    approvedAt?: string | null;
    approvedBy?: string | null;
    customerApprovalStatus?: string | null;
    customerCode?: string | null;
    customerId?: string | null;
    customerStatus?: string | null;
    customerType?: string | null;
  } | null;
  employee?: {
    employeeCode?: string | null;
    employeeId?: string | null;
    employeeStatus?: string | null;
    hiredAt?: string | null;
    jobTitle?: string | null;
  } | null;
  profile?: ApprovalProfileInfo | null;
  request?: ApprovalRequestInfo | null;
};

export type OnboardingApprovalFilters = {
  keyword?: string;
  status?: OnboardingApprovalStatus;
};

const endpointByKind: Record<OnboardingApprovalKind, string> = {
  customer: apiEndpoints.operations.customerOnboardingApprovals,
  "internal-employee": apiEndpoints.operations.internalEmployeeOnboardingApprovals,
  "system-admin": apiEndpoints.operations.systemAdminOnboardingApprovals,
};

function toQueryString(filters: OnboardingApprovalFilters = {}) {
  const searchParams = new URLSearchParams();
  if (filters.keyword?.trim()) searchParams.set("keyword", filters.keyword.trim());
  if (filters.status) searchParams.set("status", filters.status);
  const query = searchParams.toString();
  return query ? `?${query}` : "";
}

export async function fetchOnboardingApprovals(kind: OnboardingApprovalKind, filters: OnboardingApprovalFilters = {}) {
  const response = await apiClient<ApiResponse<OnboardingApprovalResponse[]>>(
    `${endpointByKind[kind]}${toQueryString(filters)}`,
  );
  return response.data ?? [];
}

export async function reviewOnboardingApproval(
  kind: OnboardingApprovalKind,
  approvalRequestId: string,
  decision: "approve" | "reject",
  note?: string,
) {
  const response = await apiClient<ApiResponse<OnboardingApprovalResponse>>(
    `${endpointByKind[kind]}/${approvalRequestId}/${decision}`,
    {
      body: { note: note?.trim() || null },
      method: "PATCH",
    },
  );
  return response.data;
}

export async function fetchMyOnboardingApproval(kind: OnboardingApprovalKind) {
  const response = await apiClient<ApiResponse<OnboardingApprovalResponse>>(`${endpointByKind[kind]}/me`);
  return response.data;
}

export async function resubmitMyOnboardingApproval(kind: OnboardingApprovalKind) {
  const response = await apiClient<ApiResponse<OnboardingApprovalResponse>>(`${endpointByKind[kind]}/me/resubmit`, {
    method: "POST",
  });
  return response.data;
}
