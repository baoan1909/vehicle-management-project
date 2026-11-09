import { apiClient } from "@/core/api/apiClient";
import { createVnpayInvoicePayment, VNPAY_MINIMUM_AMOUNT, type VnpayPaymentResponse } from "@/features/billing/api/invoicePaymentsApi";
import { storeVnpayReturnTarget } from "@/features/billing/utils/vnpayReturnTarget";
import type { LayoutElementApi, LayoutSpaceItemApi } from "@/features/parking/api/parkingLayoutApi";

type ApiResponse<T> = { data: T; message: string; success: boolean };

export type ReservationOption = {
  zoneId: string;
  zoneName: string;
  trackingMode: "SPACE" | "CAPACITY";
  available: number;
  spaces: { parkingSpaceId: string; code: string }[];
  prepaidAmount: number;
};

export type PublicReservationLayout = {
  items: LayoutSpaceItemApi[];
  elements: LayoutElementApi[];
};

export type CustomerReservation = {
  reservationId: string;
  parkingLotId: string;
  zoneId: string;
  parkingSpaceId: string | null;
  vehicleTypeId: string;
  licensePlate: string;
  status: "PENDING_PAYMENT" | "HELD" | "CHECKED_IN" | "CANCELLED" | "EXPIRED" | "PAYMENT_REVIEW";
  invoiceId: string;
  prepaidAmount: number;
  paymentHoldExpiresAt: string;
  paidAt: string | null;
  checkInDeadline: string | null;
  qrPayload: string | null;
  qrCodeDataUrl: string | null;
  accessToken: string;
  parkingLevelName: string | null;
  zoneName: string | null;
  parkingSpaceCode: string | null;
  parkingLotName: string | null;
};

export type ParkingPlateLookup = {
  sessions: {
    parkingLotId: string;
    latitude: number | null;
    longitude: number | null;
    licensePlate: string;
    vehicleTypeName: string | null;
    checkInTime: string;
    parkingLotName: string | null;
    parkingLevelName: string | null;
    zoneName: string | null;
    parkingSpaceCode: string | null;
  }[];
  reservations: {
    parkingLotId: string;
    latitude: number | null;
    longitude: number | null;
    licensePlate: string;
    prepaidAmount: number;
    paidAt: string;
    checkInDeadline: string;
    parkingLotName: string | null;
    parkingLevelName: string | null;
    zoneName: string | null;
    parkingSpaceCode: string | null;
  }[];
};

const SAVED_RESERVATIONS_KEY = "parking.guest-reservations";

type SavedReservation = { id: string; token: string };

function savedReservations(): SavedReservation[] {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(SAVED_RESERVATIONS_KEY) ?? "[]");
    return Array.isArray(value) ? value.filter((item): item is SavedReservation =>
      typeof item?.id === "string" && typeof item?.token === "string") : [];
  } catch {
    return [];
  }
}

function rememberReservation(reservation: CustomerReservation) {
  const values = savedReservations().filter((item) => item.id !== reservation.reservationId);
  localStorage.setItem(SAVED_RESERVATIONS_KEY, JSON.stringify([
    { id: reservation.reservationId, token: reservation.accessToken }, ...values,
  ].slice(0, 20)));
}

function accessToken(id: string) {
  const token = savedReservations().find((item) => item.id === id)?.token;
  if (!token) throw new Error("Trình duyệt này không còn mã truy cập đặt chỗ. Khi đến bãi, hãy cung cấp biển số để nhân viên tra cứu.");
  return token;
}

const publicReservationPath = "/public/parking/reservations";

export function lookupParkingByPlate(licensePlate: string) {
  const query = new URLSearchParams({ licensePlate });
  return apiClient<ApiResponse<ParkingPlateLookup>>(`${publicReservationPath}/lookup?${query}`, {
    skipAuth: true,
  });
}

export function getReservationOptions(parkingLotId: string, vehicleTypeId: string) {
  const query = new URLSearchParams({ parkingLotId, vehicleTypeId });
  return apiClient<ApiResponse<ReservationOption[]>>(`${publicReservationPath}/options?${query}`, { skipAuth: true });
}

export function getPublicReservationLayout(parkingLotId: string, zoneId: string) {
  const query = new URLSearchParams({ parkingLotId });
  return apiClient<ApiResponse<PublicReservationLayout>>(
    `${publicReservationPath}/options/${zoneId}/layout?${query}`, { skipAuth: true },
  );
}

export function createCustomerReservation(input: {
  parkingLotId: string;
  zoneId: string;
  parkingSpaceId: string | null;
  vehicleTypeId: string;
  licensePlate: string;
  policyAccepted: boolean;
}) {
  return apiClient<ApiResponse<{ reservation: CustomerReservation; paymentUrl: string; paymentMethod: "VNPAY" }>>(publicReservationPath, {
    method: "POST",
    body: input,
    skipAuth: true,
  }).then((response) => {
    rememberReservation(response.data.reservation);
    return response;
  });
}

export function prepareReservationReturn(paymentMethod: "VNPAY", reservationId: string, returnToNearby = false) {
  const target = returnToNearby
    ? `/parking-lots/nearby?reservationId=${encodeURIComponent(reservationId)}`
    : `/reservations/${reservationId}`;
  if (paymentMethod === "VNPAY") storeVnpayReturnTarget(target);
}

export function getCustomerReservation(id: string) {
  return apiClient<ApiResponse<CustomerReservation>>(`${publicReservationPath}/${id}`, {
    headers: { "X-Reservation-Access": accessToken(id) },
    skipAuth: true,
  });
}

export function getOwnedReservation(id: string) {
  return apiClient<ApiResponse<CustomerReservation>>(`/parking/reservations/${id}`);
}

export function getMyReservations() {
  return apiClient<ApiResponse<CustomerReservation[]>>("/parking/reservations/mine");
}

export async function getSavedReservations() {
  const values = await Promise.allSettled(savedReservations().map((item) => getCustomerReservation(item.id)));
  return values.flatMap((value) => value.status === "fulfilled" ? [value.value.data] : []);
}

export function cancelCustomerReservation(id: string) {
  return apiClient<ApiResponse<CustomerReservation>>(`${publicReservationPath}/${id}/cancel`, {
    method: "POST",
    headers: { "X-Reservation-Access": accessToken(id) },
    skipAuth: true,
  });
}

export function cancelOwnedReservation(id: string) {
  return apiClient<ApiResponse<CustomerReservation>>(`/parking/reservations/${id}/cancel`, { method: "POST" });
}

export async function startReservationPayment(reservation: CustomerReservation, owned = false, returnToNearby = false) {
  if (reservation.prepaidAmount < VNPAY_MINIMUM_AMOUNT) {
    throw new Error("Thanh toán đặt chỗ dưới 10.000đ đang tạm chưa hỗ trợ.");
  }
  if (owned) {
    const target = `/customer/reservations/${reservation.reservationId}`;
    return (await createVnpayInvoicePayment(reservation.invoiceId, target)).data.paymentUrl;
  }
  rememberReservation(reservation);
  const target = returnToNearby
    ? `/parking-lots/nearby?reservationId=${encodeURIComponent(reservation.reservationId)}`
    : `/reservations/${reservation.reservationId}`;
  const path = `${publicReservationPath}/${reservation.reservationId}/payments`;
  const headers = { "X-Reservation-Access": reservation.accessToken };
  const response = await apiClient<ApiResponse<VnpayPaymentResponse>>(`${path}/vnpay`, {
    method: "POST", body: { locale: "vn" }, headers, skipAuth: true,
  });
  storeVnpayReturnTarget(target);
  return response.data.paymentUrl;
}
