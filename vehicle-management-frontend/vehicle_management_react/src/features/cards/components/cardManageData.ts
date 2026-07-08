export type CardInventoryStatus = "available" | "assigned" | "in_use" | "pending" | "lost" | "blocked";
export type CardSubscriptionState = "none" | "active" | "pending" | "expired";
export type CardLostState = "none" | "open";
export type CardStatusTabValue = "all" | "available" | "assigned" | "in_use" | "lost" | "blocked";

export interface CardManageRecord {
  id: string;
  cardCode: string;
  uid: string;
  cardTypeLabel: string;
  vehicleType: string;
  customerName: string | null;
  phoneNumber: string | null;
  licensePlate: string | null;
  inventoryStatus: CardInventoryStatus;
  inventoryStatusLabel: string;
  subscriptionState: CardSubscriptionState;
  subscriptionStateLabel: string;
  lostCardState: CardLostState;
  lostCardStateLabel: string;
  updatedDate: string;
  updatedTime: string;
}

export interface CardSummaryMetric {
  label: string;
  value: string;
  delta: string;
  deltaTone: "green" | "red";
  icon: "card" | "user" | "clock" | "alert";
  accent: "blue" | "green" | "amber" | "red";
  sparkline: number[];
}

export const cardSummaryMetrics: CardSummaryMetric[] = [
  {
    label: "Thẻ sẵn sàng",
    value: "1.248",
    delta: "+12 so với hôm qua",
    deltaTone: "green",
    icon: "card",
    accent: "blue",
    sparkline: [14, 18, 12, 20, 15, 17, 13, 25, 19, 31, 22, 38]
  },
  {
    label: "Thẻ đang dùng",
    value: "856",
    delta: "+18 so với hôm qua",
    deltaTone: "green",
    icon: "user",
    accent: "green",
    sparkline: [12, 15, 11, 19, 13, 21, 16, 14, 28, 24, 34, 32]
  },
  {
    label: "Chờ duyệt vé tháng",
    value: "32",
    delta: "-5 so với hôm qua",
    deltaTone: "red",
    icon: "clock",
    accent: "amber",
    sparkline: [28, 34, 25, 31, 22, 18, 24, 20, 21, 29, 17, 19]
  },
  {
    label: "Báo mất mở",
    value: "14",
    delta: "+2 so với hôm qua",
    deltaTone: "red",
    icon: "alert",
    accent: "red",
    sparkline: [10, 13, 21, 19, 11, 17, 14, 22, 20, 26, 24, 38]
  }
];

export const cardStatusCounts: Record<CardStatusTabValue, number> = {
  all: 2172,
  available: 1248,
  assigned: 602,
  in_use: 256,
  lost: 14,
  blocked: 52
};

export const cardStatusTabs: Array<{ value: CardStatusTabValue; label: string }> = [
  { value: "all", label: "Tất cả" },
  { value: "available", label: "Sẵn sàng" },
  { value: "assigned", label: "Đã gán" },
  { value: "in_use", label: "Trong bãi" },
  { value: "lost", label: "Mất thẻ" },
  { value: "blocked", label: "Khóa" }
];

export const cardManageRecords: CardManageRecord[] = [
  {
    id: "card-001",
    cardCode: "C000123",
    uid: "04AABBCC11223344",
    cardTypeLabel: "Đăng ký",
    vehicleType: "Xe máy",
    customerName: "Nguyễn Văn An",
    phoneNumber: "0901 234 111",
    licensePlate: "59C1-123.45",
    inventoryStatus: "assigned",
    inventoryStatusLabel: "ASSIGNED",
    subscriptionState: "active",
    subscriptionStateLabel: "Đang hiệu lực",
    lostCardState: "none",
    lostCardStateLabel: "Không",
    updatedDate: "28/05/2026",
    updatedTime: "09:15"
  },
  {
    id: "card-002",
    cardCode: "V000456",
    uid: "04AABBCC55667788",
    cardTypeLabel: "Vãng lai",
    vehicleType: "Xe ô tô",
    customerName: "Trần Thị Bình",
    phoneNumber: "0901 234 567",
    licensePlate: "30A-987.65",
    inventoryStatus: "in_use",
    inventoryStatusLabel: "IN_USE",
    subscriptionState: "none",
    subscriptionStateLabel: "Không",
    lostCardState: "none",
    lostCardStateLabel: "Không",
    updatedDate: "28/05/2026",
    updatedTime: "08:47"
  },
  {
    id: "card-003",
    cardCode: "V000789",
    uid: "04AABBCC99887766",
    cardTypeLabel: "Vãng lai",
    vehicleType: "Xe máy",
    customerName: null,
    phoneNumber: null,
    licensePlate: null,
    inventoryStatus: "available",
    inventoryStatusLabel: "AVAILABLE",
    subscriptionState: "none",
    subscriptionStateLabel: "Không",
    lostCardState: "none",
    lostCardStateLabel: "Không",
    updatedDate: "28/05/2026",
    updatedTime: "08:30"
  },
  {
    id: "card-004",
    cardCode: "C000321",
    uid: "04AABBCC22334455",
    cardTypeLabel: "Đăng ký",
    vehicleType: "Xe ô tô",
    customerName: "Lê Minh Cường",
    phoneNumber: "0901 234 222",
    licensePlate: "51F-345.67",
    inventoryStatus: "pending",
    inventoryStatusLabel: "PENDING",
    subscriptionState: "pending",
    subscriptionStateLabel: "Chờ duyệt",
    lostCardState: "none",
    lostCardStateLabel: "Không",
    updatedDate: "28/05/2026",
    updatedTime: "07:55"
  },
  {
    id: "card-005",
    cardCode: "C000654",
    uid: "04AABBCC33445566",
    cardTypeLabel: "Đăng ký",
    vehicleType: "Xe máy",
    customerName: "Phạm Hoàng Dũng",
    phoneNumber: "0901 234 333",
    licensePlate: "59U1-111.22",
    inventoryStatus: "lost",
    inventoryStatusLabel: "LOST",
    subscriptionState: "expired",
    subscriptionStateLabel: "Hết hạn",
    lostCardState: "open",
    lostCardStateLabel: "Mở",
    updatedDate: "27/05/2026",
    updatedTime: "16:20"
  },
  {
    id: "card-006",
    cardCode: "V001012",
    uid: "04AABBCC66778899",
    cardTypeLabel: "Vãng lai",
    vehicleType: "Xe khác",
    customerName: null,
    phoneNumber: null,
    licensePlate: null,
    inventoryStatus: "blocked",
    inventoryStatusLabel: "BLOCKED",
    subscriptionState: "none",
    subscriptionStateLabel: "Không",
    lostCardState: "none",
    lostCardStateLabel: "Không",
    updatedDate: "27/05/2026",
    updatedTime: "10:05"
  },
  {
    id: "card-007",
    cardCode: "C000987",
    uid: "04AABBCC44556677",
    cardTypeLabel: "Đăng ký",
    vehicleType: "Xe ô tô",
    customerName: "Hoàng Thị Em",
    phoneNumber: "0901 234 444",
    licensePlate: "30G-222.33",
    inventoryStatus: "assigned",
    inventoryStatusLabel: "ASSIGNED",
    subscriptionState: "active",
    subscriptionStateLabel: "Đang hiệu lực",
    lostCardState: "none",
    lostCardStateLabel: "Không",
    updatedDate: "27/05/2026",
    updatedTime: "09:12"
  }
];
