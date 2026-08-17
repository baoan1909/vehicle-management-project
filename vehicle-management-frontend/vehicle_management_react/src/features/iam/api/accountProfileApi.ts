import { appConfig } from "@/config/env";
import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { localizeApiMessage, localizeApiResponseBody } from "@/core/api/apiMessage";
import { getValidAccessToken, refreshAccessToken } from "@/core/auth/tokenRefresh";

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
    permissionCodes?: string[];
    roleCode?: string;
    roleName?: string;
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
  const accessToken = await getValidAccessToken();

  let response = await sendAvatarUploadRequest(formData, accessToken);

  if (response.status === 401 && accessToken) {
    const refreshedToken = await refreshAccessToken();
    if (refreshedToken) {
      response = await sendAvatarUploadRequest(formData, refreshedToken);
    }
  }

  const responseBody = await response.json();

  if (!response.ok) {
    throw new Error(localizeApiMessage(responseBody?.message, response.status));
  }

  return localizeApiResponseBody(responseBody, response.status) as ApiResponse<AccountProfileStatusResponse>;
}

function sendAvatarUploadRequest(formData: FormData, accessToken: string | null) {
  return fetch(`${appConfig.apiBaseUrl}${apiEndpoints.iam.accountProfile.avatar}`, {
    body: formData,
    headers: {
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    method: "POST",
  });
}

export async function deleteMyAccountAvatar() {
  return apiClient<ApiResponse<AccountProfileStatusResponse>>(apiEndpoints.iam.accountProfile.avatar, {
    method: "DELETE",
  });
}
