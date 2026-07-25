import { apiEndpoints } from "@/core/api/apiEndpoints";
import { apiClient } from "@/core/api/apiClient";

export const VNPAY_MINIMUM_AMOUNT = 10000;

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type ParkingPaymentResponse = {
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

export function recordParkingCashPayment(invoiceId: string, amount: number, note?: string) {
  return apiClient<ApiResponse<ParkingPaymentResponse>>(
    `${apiEndpoints.billing.invoices}/${invoiceId}/payments`,
    {
      method: "POST",
      body: {
        amount,
        note: note?.trim() || "Nhân viên xác nhận đã thu tiền mặt khi checkout",
        paymentMethod: "CASH",
      },
    },
  );
}

export function createParkingVnpayPayment(invoiceId: string) {
  return apiClient<ApiResponse<VnpayPaymentResponse>>(
    `${apiEndpoints.billing.invoices}/${invoiceId}/payments/vnpay`,
    {
      method: "POST",
      body: {
        locale: "vn",
      },
    },
  );
}
