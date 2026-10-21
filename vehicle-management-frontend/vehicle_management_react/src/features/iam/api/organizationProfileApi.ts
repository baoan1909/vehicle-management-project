import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = { data: T; message: string; success: boolean; timestamp: string };

export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type VietnamAddressPayload = { provinceCode: string; districtCode: string | null; wardCode: string; addressDetail: string };

export type OrganizationProfileUpdateRequest = {
  approvalRequestId: string;
  organizationId: string;
  organizationCode: string;
  organizationName: string;
  addressDetail: string;
  provinceCode: string;
  districtCode: string | null;
  wardCode: string;
  addressDisplay: string;
  status: ApprovalStatus;
  submittedAt: string;
  note?: string | null;
  currentOrganizationName?: string | null;
  currentAddressDisplay?: string | null;
};

export type OrganizationProfile = {
  organizationId: string;
  code: string;
  name: string;
  addressDetail: string;
  provinceCode: string;
  districtCode: string | null;
  wardCode: string;
  addressDisplay: string;
  status: string;
  createdAt?: string | null;
  updatedAt?: string | null;
  representativeUsername?: string | null;
  representativeEmail?: string | null;
  totalParkingLots?: number;
  activeParkingLots?: number;
  setupParkingLots?: number;
  maintenanceParkingLots?: number;
  latestUpdateRequest?: OrganizationProfileUpdateRequest | null;
};

export function fetchMyOrganizationProfile() {
  return apiClient<ApiResponse<OrganizationProfile>>(apiEndpoints.iam.organizationProfile.mine);
}

export async function requestOrganizationProfileUpdate(payload: { name: string; structuredAddress: VietnamAddressPayload }) {
  const response = await apiClient<ApiResponse<OrganizationProfileUpdateRequest>>(apiEndpoints.iam.organizationProfile.updateRequests, {
    method: "POST", body: payload,
  });
  return response.data;
}

export async function fetchOrganizationProfileUpdateRequests(status?: ApprovalStatus) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  const response = await apiClient<ApiResponse<OrganizationProfileUpdateRequest[]>>(`${apiEndpoints.iam.organizationProfile.reviewRequests}${query}`);
  return response.data ?? [];
}

export async function reviewOrganizationProfileUpdate(approvalRequestId: string, decision: "approve" | "reject", note?: string) {
  const response = await apiClient<ApiResponse<OrganizationProfileUpdateRequest>>(
    `${apiEndpoints.iam.organizationProfile.reviewRequests}/${approvalRequestId}/${decision}`,
    { method: "POST", body: { note: note?.trim() || null } },
  );
  return response.data;
}
