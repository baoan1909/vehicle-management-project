import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { storeVnpayReturnTarget } from "@/features/billing/utils/vnpayReturnTarget";

export const VNPAY_MINIMUM_AMOUNT = 10000;

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type InvoiceSummaryResponse = {
  amount: number;
  customerId?: string | null;
  discountAmount: number;
  finalAmount: number;
  invoiceId: string;
  invoiceNo: string;
  issuedAt?: string | null;
  lostCardReportId?: string | null;
  paidAt?: string | null;
  parkingSessionId?: string | null;
  status: "UNPAID" | "PAID" | "CANCELLED" | "REFUNDED";
  subscriptionId?: string | null;
};

export type InvoicePaymentResponse = {
  amount: number;
  invoiceId: string;
  note?: string | null;
  paidAt?: string | null;
  paymentId: string;
  paymentMethod: string;
  receivedBy?: string | null;
  status: string;
  transactionRef?: string | null;
};

export type VnpayPaymentResponse = {
  expiresAt: string;
  invoiceId: string;
  paymentId: string;
  paymentUrl: string;
  transactionRef: string;
};

function buildQuery(filter: Record<string, string | undefined>) {
  const params = new URLSearchParams();
  Object.entries(filter).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });
  return params.toString();
}

export async function getSubscriptionInvoice(subscriptionId: string) {
  const query = buildQuery({ subscriptionId });
  const response = await apiClient<ApiResponse<InvoiceSummaryResponse[]>>(
    `${apiEndpoints.billing.invoices}?${query}`,
  );
  const invoices = response.data ?? [];
  return (
    invoices.find((invoice) => invoice.status === "UNPAID")
    ?? invoices.find((invoice) => invoice.status === "PAID")
    ?? invoices[0]
    ?? null
  );
}

export function recordCashInvoicePayment(invoiceId: string, amount: number, note?: string) {
  return apiClient<ApiResponse<InvoicePaymentResponse>>(
    `${apiEndpoints.billing.invoices}/${invoiceId}/payments`,
    {
      method: "POST",
      body: {
        amount,
        note: note?.trim() || "Nhân viên xác nhận đã thu tiền mặt tại quầy",
        paymentMethod: "CASH",
      },
    },
  );
}

export async function createVnpayInvoicePayment(
  invoiceId: string,
  returnPath: string,
) {
  const response = await apiClient<ApiResponse<VnpayPaymentResponse>>(
    `${apiEndpoints.billing.invoices}/${invoiceId}/payments/vnpay`,
    {
      method: "POST",
      body: { locale: "vn" },
    },
  );
  storeVnpayReturnTarget(returnPath);
  return response;
}
