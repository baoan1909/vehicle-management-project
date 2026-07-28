import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type AdminProvisionableAccountRoleCode = "SYSTEM_ADMIN" | "PARKING_MANAGER" | "EMPLOYEE" | "CUSTOMER";
export type ProvisionedAccountRoleCode = AdminProvisionableAccountRoleCode | (string & {});
export type ProvisionedAccountStatus = "ACTIVE" | "LOCKED" | "DISABLED" | "PENDING";

export type ProvisionedAccountResponse = {
  account: {
    accountId: string;
    accountStatus: ProvisionedAccountStatus;
    createdAt: string;
    email: string;
    keycloakUserId: string;
    updatedAt: string;
    username: string;
  };
  role: {
    permissionCodes: string[];
    roleCode: ProvisionedAccountRoleCode;
    roleId: string;
    roleName: string;
  };
};

export type ProvisionedAccountFilterRequest = {
  accountStatus?: ProvisionedAccountStatus;
  keyword?: string;
  roleCode?: AdminProvisionableAccountRoleCode;
};

export type CreateProvisionedAccountRequest = {
  email: string;
  fullName: string;
  roleCode: AdminProvisionableAccountRoleCode;
  username: string;
};

export type UpdateProvisionedAccountStatusRequest = {
  reason?: string;
  status: ProvisionedAccountStatus;
};

export type UpdateProvisionedAccountRoleRequest = {
  roleCode: AdminProvisionableAccountRoleCode;
};

export async function getProvisionedAccounts(filters: ProvisionedAccountFilterRequest = {}) {
  const searchParams = new URLSearchParams();
  if (filters.keyword) searchParams.set("keyword", filters.keyword);
  if (filters.roleCode) searchParams.set("roleCode", filters.roleCode);
  if (filters.accountStatus) searchParams.set("accountStatus", filters.accountStatus);

  const queryString = searchParams.toString();
  return apiClient<ApiResponse<ProvisionedAccountResponse[]>>(
    `${apiEndpoints.iam.provisionedAccounts}${queryString ? `?${queryString}` : ""}`,
  );
}

export async function createProvisionedAccount(payload: CreateProvisionedAccountRequest) {
  return apiClient<ApiResponse<ProvisionedAccountResponse>>(apiEndpoints.iam.provisionedAccounts, {
    body: payload,
    method: "POST",
  });
}

export async function updateProvisionedAccountStatus(accountId: string, payload: UpdateProvisionedAccountStatusRequest) {
  return apiClient<ApiResponse<ProvisionedAccountResponse>>(`${apiEndpoints.iam.provisionedAccounts}/${accountId}/status`, {
    body: payload,
    method: "PATCH",
  });
}

export async function updateProvisionedAccountRole(accountId: string, payload: UpdateProvisionedAccountRoleRequest) {
  return apiClient<ApiResponse<ProvisionedAccountResponse>>(`${apiEndpoints.iam.provisionedAccounts}/${accountId}/role`, {
    body: payload,
    method: "PATCH",
  });
}
