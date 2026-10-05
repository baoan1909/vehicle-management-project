import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type PartnerRegistrationStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export type VietnamAddressValue = {
  provinceCode: string;
  districtCode: string | null;
  wardCode: string;
  addressDetail: string;
};

export type PartnerRegistrationRequest = {
  email: string;
  fullName: string;
  organizationCode: string;
  organizationName: string;
  password: string;
  phoneNumber: string;
  representativeName: string;
  username: string;
};

export type PartnerRegistrationSubmission = {
  accountId: string;
  accountStatus: "PENDING";
  approvalRequestId: string;
  approvalStatus: "PENDING";
  nextAction: "VERIFY_EMAIL";
};

export type PartnerApplicationStatus = {
  accountId: string;
  accountStatus: string;
  approvalRequestId: string;
  approvalStatus: PartnerRegistrationStatus;
  emailVerified: boolean;
  nextAction: "VERIFY_EMAIL" | "COMPLETE_PROFILE" | "WAIT_FOR_REVIEW" | "REVIEW_REJECTED" | "ACCESS_PARTNER_PORTAL";
  hasCompletePersonalProfile: boolean;
  hasAvatar: boolean;
  hasPersonalAddress: boolean;
  hasOrganizationAddress: boolean;
  fullName: string;
  dateOfBirth?: string | null;
  gender?: string | null;
  phoneNumber?: string | null;
  identifyCard?: string | null;
  avatarUrl?: string | null;
  personalAddress?: (VietnamAddressValue & { addressDisplay?: string | null }) | null;
  organizationCode: string;
  organizationName: string;
  representativeName: string;
  representativePhoneNumber: string;
  organizationAddress?: (VietnamAddressValue & { addressDisplay?: string | null }) | null;
  reviewNote?: string | null;
  submittedAt: string;
};

export type PartnerRegistration = {
  approvalRequestId: string;
  createdAt: string;
  email: string;
  note?: string | null;
  organizationCode: string;
  organizationName: string;
  phoneNumber: string;
  representativeName: string;
  status: PartnerRegistrationStatus;
};

export type CompletePartnerProfileRequest = {
  fullName: string;
  dateOfBirth: string;
  gender: string;
  phoneNumber: string;
  identifyCard: string;
  personalAddress: VietnamAddressValue;
  organizationCode: string;
  organizationName: string;
  representativeName: string;
  representativePhoneNumber: string;
  organizationAddress: VietnamAddressValue;
};

export async function fetchPartnerRegistrations(status?: PartnerRegistrationStatus) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  const response = await apiClient<ApiResponse<PartnerRegistration[]>>(`${apiEndpoints.iam.partnerRegistrations}${query}`);
  return response.data ?? [];
}

export async function submitPartnerRegistration(request: PartnerRegistrationRequest) {
  return apiClient<ApiResponse<PartnerRegistrationSubmission>>(apiEndpoints.public.partnerRegistrations, {
    body: request,
    method: "POST",
    skipAuth: true,
  });
}

export async function getMyPartnerRegistrationStatus(options: { signal?: AbortSignal } = {}) {
  return apiClient<ApiResponse<PartnerApplicationStatus>>(apiEndpoints.iam.myPartnerRegistration, {
    signal: options.signal,
  });
}

export async function completePartnerProfile(request: CompletePartnerProfileRequest) {
  return apiClient<ApiResponse<PartnerApplicationStatus>>(apiEndpoints.iam.completePartnerProfile, {
    body: request,
    method: "POST",
  });
}

export async function reviewPartnerRegistration(
  approvalRequestId: string,
  decision: "approve" | "reject",
  note?: string,
) {
  const response = await apiClient<ApiResponse<PartnerRegistration>>(
    `${apiEndpoints.iam.partnerRegistrations}/${approvalRequestId}/${decision}`,
    {
      body: { note: note?.trim() || null },
      method: "POST",
    },
  );
  return response.data;
}
