import { appConfig } from "@/config/env";
import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { getAccessToken } from "@/core/auth/session";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type AccountProfileStatusResponse = {
  account?: {
    accountId?: string;
    accountStatus?: string;
    email?: string;
    keycloakUserId?: string;
    roleCode?: string;
    username?: string;
  };
  customer?: {
    customerApprovalStatus?: string;
    customerCode?: string;
    customerId?: string;
    customerStatus?: string;
    customerType?: string;
  };
  employee?: {
    employeeCode?: string;
    employeeId?: string;
    employeeStatus?: string;
    hiredAt?: string;
    jobTitle?: string;
  };
  onboardingRequired: boolean;
  profile?: {
    address?: string;
    avatarUrl?: string;
    dateOfBirth?: string;
    fullName?: string;
    gender?: string;
    identifyCard?: string;
    phoneNumber?: string;
    userProfileId?: string;
    userProfileStatus?: string;
  };
};

export type UpdateAccountProfileRequest = {
  address?: string;
  avatarUrl?: string;
  dateOfBirth?: string;
  fullName?: string;
  gender?: string;
  identifyCard?: string;
  phoneNumber?: string;
};

export type CompleteAccountProfileRequest = UpdateAccountProfileRequest;

export async function getMyAccountProfile() {
  return apiClient<ApiResponse<AccountProfileStatusResponse>>(apiEndpoints.iam.accountProfile.onboarding);
}

export async function completeMyAccountProfile(payload: CompleteAccountProfileRequest) {
  return apiClient<ApiResponse<AccountProfileStatusResponse>>(apiEndpoints.iam.accountProfile.onboarding, {
    method: "POST",
    body: payload,
  });
}

export async function updateMyAccountProfile(payload: UpdateAccountProfileRequest) {
  return apiClient<ApiResponse<AccountProfileStatusResponse>>(apiEndpoints.iam.accountProfile.profile, {
    method: "PATCH",
    body: payload,
  });
}

export async function uploadMyAccountAvatar(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  const accessToken = getAccessToken();

  const response = await fetch(`${appConfig.apiBaseUrl}${apiEndpoints.iam.accountProfile.avatar}`, {
    body: formData,
    headers: {
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    method: "POST",
  });

  const responseBody = await response.json();

  if (!response.ok) {
    throw new Error(responseBody?.message ?? `API error ${response.status}`);
  }

  return responseBody as ApiResponse<AccountProfileStatusResponse>;
}

export async function deleteMyAccountAvatar() {
  return apiClient<ApiResponse<AccountProfileStatusResponse>>(apiEndpoints.iam.accountProfile.avatar, {
    method: "DELETE",
  });
}
