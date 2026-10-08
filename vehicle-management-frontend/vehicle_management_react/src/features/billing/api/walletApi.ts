import { apiClient } from "@/core/api/apiClient";

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

export function payInvoiceByWallet(invoiceId: string) {
  return apiClient<ApiResponse<unknown>>("/billing/wallets/me/payments", {
    method: "POST",
    body: { invoiceId, idempotencyKey: crypto.randomUUID() },
  });
}

export function getPartnerWallet() {
  return apiClient<ApiResponse<WalletResponse>>("/billing/partner-wallets/current");
}

export function getPartnerAllocations(page = 0, size = 20) {
  return apiClient<ApiResponse<unknown[]>>(
    `/billing/partner-wallets/current/allocations?page=${page}&size=${size}`,
  );
}

export function requestPartnerPayout(bankAccountId: string, amount: number) {
  return apiClient<ApiResponse<unknown>>(
    `/billing/partner-wallets/current/payouts?bankAccountId=${bankAccountId}&amount=${amount}&idempotencyKey=${crypto.randomUUID()}`,
    { method: "POST" },
  );
}

export function getFinancialOverview() {
  return apiClient<ApiResponse<Record<string, number>>>("/billing/admin/finance/overview");
}
