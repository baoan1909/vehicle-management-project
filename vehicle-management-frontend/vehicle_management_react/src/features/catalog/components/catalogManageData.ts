export type CatalogStatus = "active" | "inactive";
export type CatalogStatusTabValue = "all" | CatalogStatus;

export type TicketCatalogRecord = {
  id: string;
  code: string;
  name: string;
  duration: string;
  status: CatalogStatus;
  priceRuleCount: number;
  updatedAt: string;
  updatedTime: string;
  description: string;
};

export type VehicleCatalogRecord = {
  id: string;
  code: string;
  name: string;
  description: string;
  status: CatalogStatus;
  linkedCount: number;
  priceRuleCount: number;
  updatedAt: string;
  updatedTime: string;
  icon: "motorbike" | "car" | "bike" | "scooter" | "truck" | "heavyTruck";
};

export const catalogStatusTabs = [
  { value: "all" as const, label: "Tất cả" },
  { value: "active" as const, label: "Đang hoạt động" },
  { value: "inactive" as const, label: "Ngừng dùng" }
];

export const ticketCatalogRecords: TicketCatalogRecord[] = [
  {
    id: "ticket-once",
    code: "TICKET_ONCE",
    name: "Vé lượt",
    duration: "-",
    status: "active",
    priceRuleCount: 18,
    updatedAt: "28/05/2026",
    updatedTime: "09:15",
    description: "Loại vé lượt áp dụng cho khách hàng thanh toán theo lần gửi xe."
  },
  {
    id: "ticket-month",
    code: "TICKET_MONTH",
    name: "Vé tháng",
    duration: "30 ngày",
    status: "active",
    priceRuleCount: 24,
    updatedAt: "28/05/2026",
    updatedTime: "08:47",
    description: "Vé đăng ký theo tháng, phù hợp khách hàng gửi xe thường xuyên."
  },
  {
    id: "ticket-resident",
    code: "TICKET_RESIDENT",
    name: "Vé cư dân",
    duration: "30 ngày",
    status: "active",
    priceRuleCount: 26,
    updatedAt: "28/05/2026",
    updatedTime: "08:30",
    description: "Vé ưu tiên cho cư dân đã xác minh thông tin tại khu vực bãi xe."
  },
  {
    id: "ticket-vip",
    code: "TICKET_VIP",
    name: "VIP",
    duration: "30 ngày",
    status: "active",
    priceRuleCount: 12,
    updatedAt: "28/05/2026",
    updatedTime: "07:55",
    description: "Loại vé ưu tiên dùng cho nhóm khách hàng đặc biệt."
  },
  {
    id: "ticket-day",
    code: "TICKET_DAY",
    name: "Vé ngày",
    duration: "1 ngày",
    status: "active",
    priceRuleCount: 15,
    updatedAt: "27/05/2026",
    updatedTime: "16:20",
    description: "Vé sử dụng trong ngày, phù hợp khách gửi xe ngắn hạn."
  },
  {
    id: "ticket-night",
    code: "TICKET_NIGHT",
    name: "Vé đêm",
    duration: "1 đêm",
    status: "active",
    priceRuleCount: 8,
    updatedAt: "27/05/2026",
    updatedTime: "15:12",
    description: "Loại vé áp dụng cho khung giờ gửi xe ban đêm."
  },
  {
    id: "ticket-student",
    code: "TICKET_STUDENT",
    name: "Vé sinh viên",
    duration: "30 ngày",
    status: "active",
    priceRuleCount: 9,
    updatedAt: "27/05/2026",
    updatedTime: "13:40",
    description: "Vé ưu đãi dành cho sinh viên đã xác thực hồ sơ."
  },
  {
    id: "ticket-employee",
    code: "TICKET_EMPLOYEE",
    name: "Vé nhân viên",
    duration: "30 ngày",
    status: "active",
    priceRuleCount: 10,
    updatedAt: "27/05/2026",
    updatedTime: "11:26",
    description: "Vé dành cho nhân viên nội bộ hoặc đối tác vận hành."
  },
  {
    id: "ticket-guest",
    code: "TICKET_GUEST",
    name: "Vé khách mời",
    duration: "7 ngày",
    status: "active",
    priceRuleCount: 4,
    updatedAt: "27/05/2026",
    updatedTime: "10:45",
    description: "Vé ngắn hạn dùng cho khách mời đã được phê duyệt."
  },
  {
    id: "ticket-week",
    code: "TICKET_WEEK",
    name: "Vé tuần",
    duration: "7 ngày",
    status: "inactive",
    priceRuleCount: 0,
    updatedAt: "27/05/2026",
    updatedTime: "10:05",
    description: "Loại vé tuần đã ngừng áp dụng trong cấu hình hiện tại."
  },
  {
    id: "ticket-quarter",
    code: "TICKET_QUARTER",
    name: "Vé quý",
    duration: "90 ngày",
    status: "inactive",
    priceRuleCount: 0,
    updatedAt: "27/05/2026",
    updatedTime: "09:12",
    description: "Loại vé quý được giữ lại để đối soát dữ liệu cũ."
  },
  {
    id: "ticket-year",
    code: "TICKET_YEAR",
    name: "Vé năm",
    duration: "365 ngày",
    status: "inactive",
    priceRuleCount: 1,
    updatedAt: "26/05/2026",
    updatedTime: "14:33",
    description: "Loại vé năm tạm ngừng để rà soát chính sách giá."
  }
];

export const vehicleCatalogRecords: VehicleCatalogRecord[] = [
  {
    id: "vehicle-motorbike",
    code: "MOTORBIKE",
    name: "Xe máy",
    description: "Xe mô tô, xe gắn máy 2 bánh.",
    status: "active",
    linkedCount: 1248,
    priceRuleCount: 18,
    updatedAt: "28/05/2026",
    updatedTime: "09:15",
    icon: "motorbike"
  },
  {
    id: "vehicle-car",
    code: "CAR",
    name: "Ô tô",
    description: "Ô tô dưới 9 chỗ ngồi.",
    status: "active",
    linkedCount: 856,
    priceRuleCount: 24,
    updatedAt: "28/05/2026",
    updatedTime: "08:47",
    icon: "car"
  },
  {
    id: "vehicle-bicycle",
    code: "BICYCLE",
    name: "Xe đạp",
    description: "Xe đạp, xe đạp điện.",
    status: "active",
    linkedCount: 132,
    priceRuleCount: 6,
    updatedAt: "28/05/2026",
    updatedTime: "08:30",
    icon: "bike"
  },
  {
    id: "vehicle-e-scooter",
    code: "E_SCOOTER",
    name: "Xe điện",
    description: "Xe máy điện, xe điện 2 bánh.",
    status: "active",
    linkedCount: 205,
    priceRuleCount: 8,
    updatedAt: "27/05/2026",
    updatedTime: "16:20",
    icon: "scooter"
  },
  {
    id: "vehicle-light-truck",
    code: "LIGHT_TRUCK",
    name: "Xe tải nhẹ",
    description: "Xe tải, xe bán tải dưới 2.5t.",
    status: "active",
    linkedCount: 98,
    priceRuleCount: 12,
    updatedAt: "27/05/2026",
    updatedTime: "10:05",
    icon: "truck"
  },
  {
    id: "vehicle-heavy-truck",
    code: "HEAVY_TRUCK",
    name: "Xe tải nặng",
    description: "Xe tải trên 2.5 tấn.",
    status: "inactive",
    linkedCount: 12,
    priceRuleCount: 0,
    updatedAt: "26/05/2026",
    updatedTime: "14:33",
    icon: "heavyTruck"
  }
];
