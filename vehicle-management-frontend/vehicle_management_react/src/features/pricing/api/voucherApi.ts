import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type VoucherDiscountType = "PERCENTAGE" | "FIXED_AMOUNT";
export type VoucherStatus = "DRAFT" | "ACTIVE" | "PAUSED" | "EXPIRED";

export type VoucherResponse = {
  voucherId: string;
  code: string;
  name: string;
  description: string | null;
  discountType: VoucherDiscountType;
  discountValue: number;
  maxDiscountAmount: number | null;
  minimumSubscriptionAmount: number;
  maxRedemptions: number | null;
  maxRedemptionsPerCustomer: number;
  validFrom: string;
  validTo: string;
  showOnDashboard: boolean;
  showOnSubscriptionPage: boolean;
  bannerTitle: string | null;
  bannerDescription: string | null;
  bannerPriority: number;
  status: VoucherStatus;
  createdAt: string | null;
  updatedAt: string | null;
};

export type VoucherPayload = Omit<VoucherResponse, "voucherId" | "status" | "createdAt" | "updatedAt">;

export function getVouchers() {
  return apiClient<ApiResponse<VoucherResponse[]>>(apiEndpoints.catalog.vouchers);
}

export function createVoucher(payload: VoucherPayload) {
  return apiClient<ApiResponse<VoucherResponse>>(apiEndpoints.catalog.vouchers, { method: "POST", body: payload });
}

export function updateVoucher(voucherId: string, payload: VoucherPayload) {
  return apiClient<ApiResponse<VoucherResponse>>(`${apiEndpoints.catalog.vouchers}/${voucherId}`, { method: "PUT", body: payload });
}

export function activateVoucher(voucherId: string) {
  return apiClient<ApiResponse<VoucherResponse>>(`${apiEndpoints.catalog.vouchers}/${voucherId}/activate`, { method: "PATCH" });
}

export function pauseVoucher(voucherId: string) {
  return apiClient<ApiResponse<VoucherResponse>>(`${apiEndpoints.catalog.vouchers}/${voucherId}/pause`, { method: "PATCH" });
}
