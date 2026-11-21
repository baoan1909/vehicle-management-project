import { apiClient } from "@/core/api/apiClient";
import type { SepayPaymentResponse } from "@/features/billing/api/invoicePaymentsApi";

type ApiResponse<T> = { data: T; message: string; success: boolean };

export type ParkingPrepaymentStatus = "PENDING_PAYMENT" | "PAID" | "EXPIRED";

export type ParkingPrepayment = {
  prepaymentId: string | null;
  invoiceId: string | null;
  parkingSessionId: string;
  licensePlate: string;
  cardNumber: string;
  parkingLotName: string | null;
  checkInTime: string;
  estimatedCheckOutAt: string | null;
  projectedGrossAmount: number | null;
  paidBeforeAmount: number;
  amount: number | null;
  status: ParkingPrepaymentStatus | null;
  expiresAt: string | null;
  paidAt: string | null;
};

export type ParkingPrepaymentCheckout = {
  prepayment: ParkingPrepayment;
  payment: SepayPaymentResponse;
};

const path = "/public/parking/prepayments";

export function lookupParkingPrepayment(licensePlate: string, cardNumber: string) {
  return apiClient<ApiResponse<ParkingPrepayment>>(`${path}/lookup`, {
    method: "POST",
    body: { licensePlate, cardNumber },
    skipAuth: true,
  });
}

export function createParkingPrepayment(input: {
  licensePlate: string;
  cardNumber: string;
  estimatedMinutes: number;
  policyAccepted: boolean;
}) {
  return apiClient<ApiResponse<ParkingPrepaymentCheckout>>(path, {
    method: "POST",
    body: input,
    skipAuth: true,
  });
}

export function getParkingPrepaymentStatus(
  prepaymentId: string,
  licensePlate: string,
  cardNumber: string,
) {
  return apiClient<ApiResponse<ParkingPrepayment>>(`${path}/${prepaymentId}/status`, {
    method: "POST",
    body: { licensePlate, cardNumber },
    skipAuth: true,
  });
}

export function downloadParkingPrepaymentQr(
  prepaymentId: string,
  licensePlate: string,
  cardNumber: string,
) {
  return apiClient<ApiResponse<string>>(`${path}/${prepaymentId}/qr`, {
    method: "POST",
    body: { licensePlate, cardNumber },
    skipAuth: true,
  });
}
