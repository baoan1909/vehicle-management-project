import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type CurrentAccountAccessResponse = {
  accountId?: string;
  accountStatus?: string;
  email?: string;
  employeeStatus?: string;
  permissionCodes?: string[];
  roleCode?: string;
  roleId?: string;
  username?: string;
};

export async function getMyAccountAccess(options: { signal?: AbortSignal } = {}) {
  return apiClient<ApiResponse<CurrentAccountAccessResponse>>(apiEndpoints.iam.accountProfile.currentAccess, {
    signal: options.signal,
  });
}
