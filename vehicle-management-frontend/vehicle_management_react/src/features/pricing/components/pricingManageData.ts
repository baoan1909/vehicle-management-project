export type PricePlanAppliesTo = "ALL" | "VISITOR" | "CUSTOMER";
export type PriceRuleGroup = "all" | "visitor" | "subscription" | "free";
export type PricingStatus = "active" | "expired" | "upcoming";

export type PricingMetricTone = "blue" | "green" | "orange" | "red" | "purple";

export type PricingMetric = {
  label: string;
  value: string;
  icon: "calendar" | "clock" | "x-calendar" | "folder" | "car" | "ticket" | "gift" | "layers";
  tone: PricingMetricTone;
};

export type PricePlanRecord = {
  id: string;
  code: string;
  name: string;
  description: string;
  appliesTo: PricePlanAppliesTo;
  effectiveFrom: string;
  effectiveTo: string | null;
  status: PricingStatus;
  isActive: boolean;
  updatedDate: string;
  updatedTime: string;
};

export type PriceRuleRecord = {
  id: string;
  pricePlanId: string;
  pricePlanCode: string;
  vehicleTypeId: string;
  vehicleTypeName: string;
  ticketTypeId: string;
  ticketTypeCode: string;
  ruleName: string;
  timeFrom: string | null;
  timeTo: string | null;
  basePrice: number;
  unit: string;
  lostCardFee: number;
  priority: number;
  group: PriceRuleGroup;
  status: PricingStatus;
  isActive: boolean;
};

export const pricePlanMetrics: PricingMetric[] = [
  { label: "Đang áp dụng", value: "2", icon: "calendar", tone: "green" },
  { label: "Sắp hiệu lực", value: "1", icon: "clock", tone: "orange" },
  { label: "Hết hiệu lực", value: "3", icon: "x-calendar", tone: "red" },
  { label: "Tổng kế hoạch", value: "6", icon: "folder", tone: "blue" }
];

export const priceRuleMetrics: PricingMetric[] = [
  { label: "Vãng lai", value: "4", icon: "car", tone: "blue" },
  { label: "Đăng ký", value: "6", icon: "ticket", tone: "orange" },
  { label: "Miễn phí", value: "1", icon: "gift", tone: "green" },
  { label: "Đang áp dụng", value: "11", icon: "layers", tone: "purple" }
];

export const pricePlanRecords: PricePlanRecord[] = [
  {
    id: "plan-2026-hk1",
    code: "PLAN-2026-HK1",
    name: "Bảng giá học kỳ 1",
    description: "Bảng giá áp dụng cho học kỳ 1 năm 2026.",
    appliesTo: "ALL",
    effectiveFrom: "01/06/2026",
    effectiveTo: "31/12/2026",
    status: "active",
    isActive: true,
    updatedDate: "03/07/2026",
    updatedTime: "10:15"
  },
  {
    id: "plan-he-2026",
    code: "PLAN-HE-2026",
    name: "Bảng giá hè",
    description: "Bảng giá riêng cho thời gian hè.",
    appliesTo: "VISITOR",
    effectiveFrom: "01/07/2026",
    effectiveTo: "31/08/2026",
    status: "active",
    isActive: true,
    updatedDate: "03/07/2026",
    updatedTime: "09:40"
  },
  {
    id: "plan-free-hosp",
    code: "PLAN-FREE-HOSP",
    name: "Miễn giảm bệnh viện",
    description: "Kế hoạch miễn giảm cho khách đăng ký đặc biệt.",
    appliesTo: "CUSTOMER",
    effectiveFrom: "01/06/2026",
    effectiveTo: null,
    status: "active",
    isActive: true,
    updatedDate: "02/07/2026",
    updatedTime: "16:20"
  },
  {
    id: "plan-2025-hk2",
    code: "PLAN-2025-HK2",
    name: "Bảng giá học kỳ 2/2025",
    description: "Bảng giá đã hết hiệu lực.",
    appliesTo: "ALL",
    effectiveFrom: "01/01/2025",
    effectiveTo: "31/05/2025",
    status: "expired",
    isActive: false,
    updatedDate: "31/05/2025",
    updatedTime: "23:10"
  },
  {
    id: "plan-vip-2025",
    code: "PLAN-VIP-2025",
    name: "Gói VIP đặc biệt 2025",
    description: "Kế hoạch thử nghiệm cho nhóm khách đặc biệt.",
    appliesTo: "ALL",
    effectiveFrom: "01/01/2025",
    effectiveTo: "31/12/2025",
    status: "expired",
    isActive: false,
    updatedDate: "30/12/2024",
    updatedTime: "14:05"
  },
  {
    id: "plan-2027-draft",
    code: "PLAN-2027-DRAFT",
    name: "Bảng giá dự kiến 2027",
    description: "Kế hoạch giá chuẩn bị áp dụng.",
    appliesTo: "ALL",
    effectiveFrom: "01/01/2027",
    effectiveTo: null,
    status: "upcoming",
    isActive: true,
    updatedDate: "04/07/2026",
    updatedTime: "08:30"
  }
];

export const priceRuleRecords: PriceRuleRecord[] = [
  {
    id: "rule-moto-day",
    pricePlanId: "plan-2026-hk1",
    pricePlanCode: "PLAN-2026-HK1",
    vehicleTypeId: "moto",
    vehicleTypeName: "Xe máy",
    ticketTypeId: "daily",
    ticketTypeCode: "DAILY",
    ruleName: "Xe máy vãng lai ban ngày",
    timeFrom: "06:00",
    timeTo: "19:59",
    basePrice: 4000,
    unit: "TURN",
    lostCardFee: 50000,
    priority: 10,
    group: "visitor",
    status: "active",
    isActive: true
  },
  {
    id: "rule-moto-night",
    pricePlanId: "plan-2026-hk1",
    pricePlanCode: "PLAN-2026-HK1",
    vehicleTypeId: "moto",
    vehicleTypeName: "Xe máy",
    ticketTypeId: "daily",
    ticketTypeCode: "DAILY",
    ruleName: "Xe máy vãng lai ban đêm",
    timeFrom: "20:00",
    timeTo: "05:59",
    basePrice: 8000,
    unit: "TURN",
    lostCardFee: 50000,
    priority: 10,
    group: "visitor",
    status: "active",
    isActive: true
  },
  {
    id: "rule-moto-monthly",
    pricePlanId: "plan-2026-hk1",
    pricePlanCode: "PLAN-2026-HK1",
    vehicleTypeId: "moto",
    vehicleTypeName: "Xe máy",
    ticketTypeId: "monthly",
    ticketTypeCode: "MONTHLY",
    ruleName: "Xe máy vé tháng",
    timeFrom: null,
    timeTo: null,
    basePrice: 80000,
    unit: "MONTH",
    lostCardFee: 120000,
    priority: 20,
    group: "subscription",
    status: "active",
    isActive: true
  },
  {
    id: "rule-free-hospital",
    pricePlanId: "plan-free-hosp",
    pricePlanCode: "PLAN-FREE-HOSP",
    vehicleTypeId: "moto",
    vehicleTypeName: "Xe máy",
    ticketTypeId: "free",
    ticketTypeCode: "FREE",
    ruleName: "Vé FREE bệnh viện",
    timeFrom: null,
    timeTo: null,
    basePrice: 0,
    unit: "MONTH",
    lostCardFee: 0,
    priority: 30,
    group: "free",
    status: "active",
    isActive: true
  },
  {
    id: "rule-car-monthly",
    pricePlanId: "plan-2026-hk1",
    pricePlanCode: "PLAN-2026-HK1",
    vehicleTypeId: "car",
    vehicleTypeName: "Ô tô",
    ticketTypeId: "monthly",
    ticketTypeCode: "MONTHLY",
    ruleName: "Ô tô vé tháng",
    timeFrom: null,
    timeTo: null,
    basePrice: 350000,
    unit: "MONTH",
    lostCardFee: 200000,
    priority: 20,
    group: "subscription",
    status: "active",
    isActive: true
  },
  {
    id: "rule-moto-quarter",
    pricePlanId: "plan-2026-hk1",
    pricePlanCode: "PLAN-2026-HK1",
    vehicleTypeId: "moto",
    vehicleTypeName: "Xe máy",
    ticketTypeId: "quarterly",
    ticketTypeCode: "QUARTERLY",
    ruleName: "Xe máy vé quý",
    timeFrom: null,
    timeTo: null,
    basePrice: 220000,
    unit: "QUARTER",
    lostCardFee: 120000,
    priority: 20,
    group: "subscription",
    status: "active",
    isActive: true
  }
];
