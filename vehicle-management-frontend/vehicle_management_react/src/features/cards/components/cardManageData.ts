export type CardInventoryStatus = "available" | "assigned" | "in_use" | "reserved" | "lost" | "blocked" | "retired";
export type CardSubscriptionState = "none" | "active" | "pending" | "expired";
export type CardLostState = "none" | "open";
export type CardStatusTabValue = "all" | CardInventoryStatus;

export interface CardManageRecord {
  blockedReason: string | null;
  blockedBy: string | null;
  blockedPreviousStatus: string | null;
  cardCode: string;
  cardReceiptDate: string | null;
  cardTypeId: string | null;
  cardTypeLabel: string;
  customerApprovalStatus: string | null;
  customerCode: string | null;
  customerEmail: string | null;
  customerId: string | null;
  customerName: string | null;
  customerStatus: string | null;
  customerType: string | null;
  customerVehicleId: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  id: string;
  inventoryStatus: CardInventoryStatus;
  inventoryStatusLabel: string;
  licensePlate: string | null;
  lostCardState: CardLostState;
  lostCardStateLabel: string;
  phoneNumber: string | null;
  registeredVehicleTypeCode: string | null;
  registeredVehicleTypeId: string | null;
  registeredVehicleTypeName: string | null;
  requestedEffectiveFrom: string | null;
  subscriptionId: string | null;
  subscriptionPrice: number | null;
  subscriptionState: CardSubscriptionState;
  subscriptionStateLabel: string;
  subscriptionStatus: string | null;
  ticketTypeCode: string | null;
  ticketTypeId: string | null;
  ticketTypeLabel: string | null;
  ticketTypeName: string | null;
  uid: string;
  updatedDate: string;
  updatedTime: string;
  vehicleBrand: string | null;
  vehicleColor: string | null;
  vehicleTypeLabel: string | null;
}

export interface CardSummaryMetric {
  label: string;
  value: string;
  icon: "card" | "user" | "clock" | "alert";
  accent: "blue" | "green" | "amber" | "red";
}

export const cardStatusTabs: Array<{ value: CardStatusTabValue; label: string }> = [
  { value: "all", label: "Tất cả" },
  { value: "available", label: "Sẵn sàng" },
  { value: "assigned", label: "Đã gán" },
  { value: "in_use", label: "Trong bãi" },
  { value: "lost", label: "Mất thẻ" },
  { value: "blocked", label: "Khóa" },
  { value: "retired", label: "Ngưng sử dụng" },
];
