import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type InvoiceStatus = "UNPAID" | "PAID" | "CANCELLED" | "REFUNDED";
export type PaymentMethod = "CASH" | "QR" | "BANK_TRANSFER" | "MOMO" | "VNPAY";
export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "REFUNDED";
export type InvoiceSource = "PARKING_SESSION" | "SUBSCRIPTION" | "LOST_CARD" | "MANUAL";

export type InvoiceManagementItem = {
  invoiceId: string;
  invoiceNo: string;
  customerId: string | null;
  customerName: string;
  licensePlate: string | null;
  source: InvoiceSource;
  sourceId: string | null;
  amount: number;
  discountAmount: number;
  finalAmount: number;
  status: InvoiceStatus;
  paymentMethod: PaymentMethod | null;
  paymentStatus: PaymentStatus | null;
  transactionRef: string | null;
  issuedAt: string | null;
  paidAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type InvoiceManagementPage = {
  items: InvoiceManagementItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type InvoiceManagementSummary = {
  total: number;
  unpaid: number;
  paid: number;
  cancelled: number;
  refunded: number;
};

export type InvoiceLineItem = {
  code: string;
  description: string;
  amount: number;
};

export type InvoicePayment = {
  paymentId: string;
  invoiceId: string;
  paymentMethod: PaymentMethod;
  amount: number;
  transactionRef: string | null;
  status: PaymentStatus;
  paidAt: string | null;
  receivedBy: string | null;
  note: string | null;
};

export type InvoiceManagementDetail = {
  invoice: InvoiceManagementItem;
  lineItems: InvoiceLineItem[];
  payments: InvoicePayment[];
};

export type InvoiceManagementFilter = {
  status?: InvoiceStatus;
  paymentMethod?: PaymentMethod;
  fromDate?: string;
  toDate?: string;
  keyword?: string;
  page?: number;
  size?: number;
};

function buildQuery(filter: InvoiceManagementFilter) {
  const params = new URLSearchParams();
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });
  const query = params.toString();
  return query ? `?${query}` : "";
}

export function getInvoiceManagementList(filter: InvoiceManagementFilter = {}) {
  return apiClient<ApiResponse<InvoiceManagementPage>>(
    `${apiEndpoints.billing.invoices}/management${buildQuery(filter)}`,
  );
}

export function getInvoiceManagementSummary() {
  return apiClient<ApiResponse<InvoiceManagementSummary>>(
    `${apiEndpoints.billing.invoices}/management/summary`,
  );
}

export function getInvoiceManagementDetail(invoiceId: string) {
  return apiClient<ApiResponse<InvoiceManagementDetail>>(
    `${apiEndpoints.billing.invoices}/management/${invoiceId}`,
  );
}
