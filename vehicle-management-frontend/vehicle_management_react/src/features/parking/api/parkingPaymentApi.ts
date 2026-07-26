import {
  createVnpayInvoicePayment,
  recordCashInvoicePayment,
  VNPAY_MINIMUM_AMOUNT,
} from "@/features/billing/api/invoicePaymentsApi";

export { VNPAY_MINIMUM_AMOUNT };

export function recordParkingCashPayment(invoiceId: string, amount: number, note?: string) {
  return recordCashInvoicePayment(
    invoiceId,
    amount,
    note?.trim() || "Nhân viên xác nhận đã thu tiền mặt khi checkout",
  );
}

export function createParkingVnpayPayment(invoiceId: string) {
  return createVnpayInvoicePayment(invoiceId, "/admin/swipe");
}
