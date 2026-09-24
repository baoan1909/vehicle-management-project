import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type PartnerRegistrationStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export type PartnerRegistration = {
  address: string;
  approvalRequestId: string;
  createdAt: string;
  email: string;
  expectedParkingLotCount: number;
  note?: string | null;
  organizationCode: string;
  organizationName: string;
  parkingOperationDescription?: string | null;
  phoneNumber: string;
  representativeName: string;
  status: PartnerRegistrationStatus;
};

export async function fetchPartnerRegistrations(status?: PartnerRegistrationStatus) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  const response = await apiClient<ApiResponse<PartnerRegistration[]>>(`${apiEndpoints.iam.partnerRegistrations}${query}`);
  return response.data ?? [];
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
