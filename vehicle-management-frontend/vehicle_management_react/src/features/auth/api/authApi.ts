import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type RegisterAccountRequest = {
  username: string;
  email: string;
  password: string;
};

export type RegisterAccountResponse = {
  accountId: string;
  userProfileId: string;
  customerId: string;
  accountStatus: string;
  nextAction: string;
};

export type ForgotPasswordRequest = {
  email: string;
};

export async function registerAccount(payload: RegisterAccountRequest) {
  return apiClient<ApiResponse<RegisterAccountResponse>>(apiEndpoints.auth.register, {
    method: "POST",
    body: payload,
  });
}

export async function requestPasswordReset(payload: ForgotPasswordRequest) {
  return apiClient<ApiResponse<null>>(apiEndpoints.auth.forgotPassword, {
    method: "POST",
    body: payload,
  });
}
