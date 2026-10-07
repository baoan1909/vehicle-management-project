import { appConfig } from "@/config/env";
import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { localizeApiMessage, localizeApiResponseBody } from "@/core/api/apiMessage";
import { getValidAccessToken, refreshAccessToken } from "@/core/auth/tokenRefresh";
import type { VietnamAddressValue } from "@/components/ui";

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
    addressDetail?: string;
    provinceCode?: string;
    wardCode?: string;
    districtCode?: string | null;
    addressDisplay?: string;
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
  structuredAddress?: VietnamAddressValue;
  avatarUrl?: string;
  dateOfBirth?: string;
  fullName?: string;
  gender?: string;
  identifyCard?: string;
  phoneNumber?: string;
};

export type AvatarModerationStatus = {
  avatarId: string;
  approvalRequestId: string;
  userProfileId: string;
  ownerAccountId: string;
  approvalStatus: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  reviewNote?: string | null;
  displayedAvatarUrl?: string | null;
  candidatePreviewUrl?: string | null;
  submittedAt: string;
};

export type CompleteAccountProfileRequest = UpdateAccountProfileRequest;

export type SocialAccountBootstrapResponse = {
  accountId: string;
  accountStatus: string;
  roleCode: "CUSTOMER";
  provider: "GOOGLE";
  created: boolean;
};

export async function getMyAccountProfile() {
  return apiClient<ApiResponse<AccountProfileStatusResponse>>(apiEndpoints.iam.accountProfile.onboarding);
}

export async function bootstrapSocialAccount() {
  return apiClient<ApiResponse<SocialAccountBootstrapResponse>>(
    apiEndpoints.iam.accountProfile.socialBootstrap,
    { method: "POST" },
  );
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

export async function getMyAvatarModerationStatus() {
  return apiClient<ApiResponse<AvatarModerationStatus | null>>(
    apiEndpoints.iam.accountProfile.avatarStatus,
  );
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
