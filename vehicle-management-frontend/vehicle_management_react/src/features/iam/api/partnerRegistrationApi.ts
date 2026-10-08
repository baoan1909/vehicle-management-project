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
  username: string;
};

export type PartnerRegistrationSubmission = {
  accountId: string;
  accountStatus: "PENDING";
  approvalRequestId: string;
  approvalStatus: "PENDING";
  nextAction: "VERIFY_EMAIL";
};

export type PartnerRegistration = {
  approvalRequestId: string;
  submittedAt: string;
  applicantFullName: string;
  applicantPhoneNumber: string;
  applicantEmail: string;
  note?: string | null;
  organizationCode: string;
  organizationName: string;
  organizationAddressDisplay: string;
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
