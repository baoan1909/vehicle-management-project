import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type WalletResponse = {
  walletId: string;
  ownerType: string;
  currency: string;
  availableBalance: number;
  pendingBalance: number;
  heldBalance: number;
  status: string;
};

export type WalletTransactionResponse = {
  financialTransactionId: string;
  transactionCode: string;
  transactionType: string;
  status: string;
  currency: string;
  occurredAt: string;
  referenceId?: string | null;
};

export type WalletTopupResponse = {
  topupOrderId: string;
  walletId: string;
  amount: number;
  currency: string;
  status: string;
  transactionRef: string;
  paymentUrl: string;
  expiresAt: string;
  completedAt?: string | null;
};

export type RevenueAllocationResponse = {
  revenueAllocationId: string;
  paymentId: string;
  invoiceId: string;
  parkingLotId?: string | null;
  organizationId?: string | null;
  voucherId?: string | null;
  grossAmount: number;
  discountAmount: number;
  customerPaidAmount: number;
  sponsoredDiscountAmount: number;
  platformFeeAmount: number;
  voucherCommissionAmount: number;
  partnerPayableAmount: number;
  gatewayFeeAmount: number;
  fundingSourceSnapshot?: string | null;
  status: string;
  availableAt?: string | null;
  settledAt?: string | null;
  createdAt: string;
};

export type PayoutRequestResponse = {
  payoutRequestId: string;
  walletId: string;
  organizationId: string;
  bankAccountId: string;
  amount: number;
  currency: string;
  status: string;
  requestedBy: string;
  requestedAt: string;
  decidedBy?: string | null;
  decidedAt?: string | null;
  failureReason?: string | null;
};

export const QUICK_TOPUP_AMOUNTS = [50000, 100000, 200000, 500000, 1000000];

export function getMyWallet() {
  return apiClient<ApiResponse<WalletResponse>>("/billing/wallets/me");
}

export function getMyWalletTransactions(page = 0, size = 20) {
  return apiClient<ApiResponse<WalletTransactionResponse[]>>(
    `/billing/wallets/me/transactions?page=${page}&size=${size}`,
  );
}

export function createWalletTopup(amount: number) {
  return apiClient<ApiResponse<WalletTopupResponse>>("/billing/wallets/me/topups", {
    method: "POST",
    body: { amount, idempotencyKey: crypto.randomUUID() },
  });
}

export function getTopupOrder(topupOrderId: string) {
  return apiClient<ApiResponse<WalletTopupResponse>>(`/billing/wallets/me/topups/${topupOrderId}`);
}

export function getMyWalletTopups(page = 0, size = 100) {
  return apiClient<ApiResponse<WalletTopupResponse[]>>(
    `/billing/wallets/me/topups?page=${page}&size=${size}`,
  );
}

export function payInvoiceByWallet(invoiceId: string, idempotencyKey: string = crypto.randomUUID()) {
  return apiClient<ApiResponse<unknown>>("/billing/wallets/me/payments", {
    method: "POST",
    body: { invoiceId, idempotencyKey },
  });
}

export function getPartnerWallet() {
  return apiClient<ApiResponse<WalletResponse>>(apiEndpoints.billing.partnerWalletCurrent);
}

export function getPartnerWalletTransactions(page = 0, size = 20) {
  return apiClient<ApiResponse<WalletTransactionResponse[]>>(
    `${apiEndpoints.billing.partnerWalletTransactions}?page=${page}&size=${size}`,
  );
}

export function getPartnerAllocations(page = 0, size = 100) {
  return apiClient<ApiResponse<RevenueAllocationResponse[]>>(
    `${apiEndpoints.billing.partnerAllocations}?page=${page}&size=${size}`,
  );
}

export function getPartnerPayouts(page = 0, size = 20) {
  return apiClient<ApiResponse<PayoutRequestResponse[]>>(
    `${apiEndpoints.billing.partnerPayouts}?page=${page}&size=${size}`,
  );
}

export function requestPartnerPayout(bankAccountId: string, amount: number) {
  return apiClient<ApiResponse<PayoutRequestResponse>>(
    `${apiEndpoints.billing.partnerPayouts}?bankAccountId=${encodeURIComponent(bankAccountId)}&amount=${amount}&idempotencyKey=${crypto.randomUUID()}`,
    { method: "POST" },
  );
}

export type AdminWalletResponse = {
  walletId: string;
  ownerType: string;
  customerId?: string | null;
  organizationId?: string | null;
  walletPurpose?: string | null;
  currency: string;
  availableBalance: number;
  pendingBalance: number;
  heldBalance: number;
  status: string;
  updatedAt?: string | null;
};

export function getFinancialOverview() {
  return apiClient<ApiResponse<Record<string, number>>>("/billing/admin/finance/overview");
}

export function listAdminWallets(page = 0, size = 50) {
  return apiClient<ApiResponse<AdminWalletResponse[]>>(
    `/billing/admin/wallets?page=${page}&size=${size}`,
  );
}

export function getAdminWalletTransactions(walletId: string, page = 0, size = 20) {
  return apiClient<ApiResponse<WalletTransactionResponse[]>>(
    `/billing/admin/wallets/${walletId}/transactions?page=${page}&size=${size}`,
  );
}

export function lockWallet(walletId: string) {
  return apiClient<ApiResponse<unknown>>(`/billing/admin/wallets/${walletId}/lock`, {
    method: "PATCH",
  });
}

export function unlockWallet(walletId: string) {
  return apiClient<ApiResponse<unknown>>(`/billing/admin/wallets/${walletId}/unlock`, {
    method: "PATCH",
  });
}

export function requestWalletAdjustment(walletId: string, amount: number, direction: "CREDIT" | "DEBIT", reason: string) {
  return apiClient<ApiResponse<unknown>>("/billing/admin/wallet-adjustments", {
    method: "POST",
    body: { walletId, amount, direction, reason },
  });
}
