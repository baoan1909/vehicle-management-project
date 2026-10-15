import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type PlatformFeeTransactionType = "SUBSCRIPTION_PURCHASE";
export type PlatformFeeCalculationType = "PERCENTAGE";
export type PlatformFeeBasis = "GROSS_AMOUNT" | "AFTER_DISCOUNT";
export type PlatformFeeChargedTo = "PARTNER" | "CUSTOMER" | "PLATFORM";
export type PlatformFeeTypeStatus = "DRAFT" | "ACTIVE" | "INACTIVE" | "ARCHIVED";
export type PlatformFeeRuleVersionStatus = "DRAFT" | "ACTIVE" | "INACTIVE" | "ARCHIVED";

export type PlatformFeeTypeResponse = {
  platformFeeTypeId: string;
  feeCode: string;
  name: string;
  description: string | null;
  transactionType: PlatformFeeTransactionType;
  calculationType: PlatformFeeCalculationType;
  feeBasis: PlatformFeeBasis;
  chargedTo: PlatformFeeChargedTo;
  stackable: boolean;
  status: PlatformFeeTypeStatus;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
  archivedAt: string | null;
  archivedBy: string | null;
};

export type PlatformFeeVersionResponse = {
  platformFeeRuleVersionId: string;
  platformFeeTypeId: string;
  version: number;
  previousVersionId: string | null;
  /** Tỷ lệ dạng phân số, ví dụ 0.1 = 10%. */
  feeRate: number;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  status: PlatformFeeRuleVersionStatus;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
  activatedAt: string | null;
  activatedBy: string | null;
  deactivatedAt: string | null;
  deactivatedBy: string | null;
  archivedAt: string | null;
  archivedBy: string | null;
};

export type PlatformFeeTypeDetailResponse = {
  feeType: PlatformFeeTypeResponse;
  effectiveVersion: PlatformFeeVersionResponse | null;
  upcomingVersion: PlatformFeeVersionResponse | null;
  totalVersions: number;
  recentVersions: PlatformFeeVersionResponse[];
};

export type PlatformFeePageResponse = {
  items: PlatformFeeTypeResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type PlatformFeeConfigurationResponse = {
  maxPartnerTotalRate: number;
};

export type PlatformFeePreviewLineResponse = {
  feeTypeId: string;
  ruleVersionId: string;
  feeCode: string;
  feeName: string;
  version: number;
  feeRate: number;
  feeBasis: string | null;
  chargedTo: string | null;
  basisAmount: number;
  feeAmount: number;
};

export type PlatformFeePreviewResponse = {
  transactionType: PlatformFeeTransactionType;
  effectiveAt: string;
  grossAmount: number;
  customerPaidAmount: number;
  feeLines: PlatformFeePreviewLineResponse[];
  totalFeeAmount: number;
  effectiveTotalRate: number;
  partnerRemainingAmount: number;
};

export type PlatformFeeTypeFilter = {
  keyword?: string;
  status?: PlatformFeeTypeStatus;
  transactionType?: PlatformFeeTransactionType;
  feeBasis?: PlatformFeeBasis;
  page?: number;
  size?: number;
};

export type CreatePlatformFeeTypeRequest = {
  feeCode: string;
  name: string;
  description?: string | null;
  transactionType: PlatformFeeTransactionType;
  calculationType: PlatformFeeCalculationType;
  feeBasis: PlatformFeeBasis;
  chargedTo: PlatformFeeChargedTo;
  stackable: boolean;
};

export type UpdatePlatformFeeTypeRequest = {
  name: string;
  description?: string | null;
  feeBasis: PlatformFeeBasis;
  stackable: boolean;
};

export type SavePlatformFeeVersionRequest = {
  feeRate: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
};

export type PreviewPlatformFeesRequest = {
  transactionType: PlatformFeeTransactionType;
  effectiveAt: string;
  grossAmount: number;
  customerPaidAmount: number;
};

function buildQuery(filter: PlatformFeeTypeFilter) {
  const params = new URLSearchParams();
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });
  const query = params.toString();
  return query ? `?${query}` : "";
}

const base = () => apiEndpoints.billing.platformFees;

export function searchPlatformFeeTypes(filter: PlatformFeeTypeFilter = {}) {
  return apiClient<ApiResponse<PlatformFeePageResponse>>(`${base()}${buildQuery(filter)}`);
}

export function getPlatformFeeConfiguration() {
  return apiClient<ApiResponse<PlatformFeeConfigurationResponse>>(`${base()}/configuration`);
}

export function createPlatformFeeType(payload: CreatePlatformFeeTypeRequest) {
  return apiClient<ApiResponse<PlatformFeeTypeResponse>>(base(), {
    body: payload,
    method: "POST",
  });
}

export function getPlatformFeeTypeDetail(feeTypeId: string) {
  return apiClient<ApiResponse<PlatformFeeTypeDetailResponse>>(`${base()}/${feeTypeId}`);
}

export function updateDraftPlatformFeeType(feeTypeId: string, payload: UpdatePlatformFeeTypeRequest) {
  return apiClient<ApiResponse<PlatformFeeTypeResponse>>(`${base()}/${feeTypeId}`, {
    body: payload,
    method: "PUT",
  });
}

export function activatePlatformFeeType(feeTypeId: string) {
  return apiClient<ApiResponse<PlatformFeeTypeResponse>>(`${base()}/${feeTypeId}/activate`, {
    method: "PATCH",
  });
}

export function deactivatePlatformFeeType(feeTypeId: string) {
  return apiClient<ApiResponse<PlatformFeeTypeResponse>>(`${base()}/${feeTypeId}/deactivate`, {
    method: "PATCH",
  });
}

export function archivePlatformFeeType(feeTypeId: string) {
  return apiClient<ApiResponse<PlatformFeeTypeResponse>>(`${base()}/${feeTypeId}`, {
    method: "DELETE",
  });
}

export function getPlatformFeeVersionHistory(feeTypeId: string) {
  return apiClient<ApiResponse<PlatformFeeVersionResponse[]>>(`${base()}/${feeTypeId}/versions`);
}

export function createPlatformFeeVersion(feeTypeId: string, payload: SavePlatformFeeVersionRequest) {
  return apiClient<ApiResponse<PlatformFeeVersionResponse>>(`${base()}/${feeTypeId}/versions`, {
    body: payload,
    method: "POST",
  });
}

export function updateDraftPlatformFeeVersion(feeTypeId: string, versionId: string, payload: SavePlatformFeeVersionRequest) {
  return apiClient<ApiResponse<PlatformFeeVersionResponse>>(`${base()}/${feeTypeId}/versions/${versionId}`, {
    body: payload,
    method: "PUT",
  });
}

export function activatePlatformFeeVersion(feeTypeId: string, versionId: string) {
  return apiClient<ApiResponse<PlatformFeeVersionResponse>>(`${base()}/${feeTypeId}/versions/${versionId}/activate`, {
    method: "PATCH",
  });
}

export function deactivatePlatformFeeVersion(feeTypeId: string, versionId: string) {
  return apiClient<ApiResponse<PlatformFeeVersionResponse>>(`${base()}/${feeTypeId}/versions/${versionId}/deactivate`, {
    method: "PATCH",
  });
}

export function archivePlatformFeeVersion(feeTypeId: string, versionId: string) {
  return apiClient<ApiResponse<PlatformFeeVersionResponse>>(`${base()}/${feeTypeId}/versions/${versionId}`, {
    method: "DELETE",
  });
}

export function previewPlatformFees(payload: PreviewPlatformFeesRequest) {
  return apiClient<ApiResponse<PlatformFeePreviewResponse>>(`${base()}/preview`, {
    body: payload,
    method: "POST",
  });
}
