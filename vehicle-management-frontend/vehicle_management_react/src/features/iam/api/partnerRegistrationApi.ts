import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type PartnerRegistrationStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

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
  nextAction: "VERIFY_EMAIL" | "WAIT_FOR_REVIEW" | "REVIEW_REJECTED" | "ACCESS_PARTNER_PORTAL";
  organizationCode: string;
  organizationName: string;
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
