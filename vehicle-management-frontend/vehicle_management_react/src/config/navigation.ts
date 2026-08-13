import type { AdminSidebarEntry } from "../shared/types/common";

export const adminNavigation: AdminSidebarEntry[] = [
  {
    kind: "link",
    label: "Trang chủ",
    to: "/admin/dashboard",
    matches: ["/admin/dashboard"],
    icon: "dashboard",
  },
  { kind: "divider" },
  {
    kind: "group",
    label: "Quản lý vào ra",
    icon: "swipe",
    defaultExpanded: true,
    items: [
      { label: "Vào/Ra", to: "/admin/swipe", matches: ["/admin/swipe"] },
      { label: "Phiên gửi xe", to: "/admin/swipe/sessions", matches: ["/admin/swipe/sessions"] },
    ],
  },
  {
    kind: "group",
    label: "Quản lý thẻ",
    icon: "card",
    defaultExpanded: true,
    items: [
      { label: "Thẻ", to: "/admin/card", matches: ["/admin/card"] },
      { label: "Thẻ bị mất", to: "/admin/lost", matches: ["/admin/lost"] },
    ],
  },
  {
    kind: "group",
    label: "Bãi xe & thiết bị",
    icon: "parking",
    items: [
      { label: "Bãi xe", to: "/admin/parking-lots", matches: ["/admin/parking-lots"] },
      { label: "Thiết bị", to: "/admin/devices", matches: ["/admin/devices"] },
    ],
  },
  {
    kind: "group",
    label: "Hỗ trợ & CSKH",
    icon: "support",
    items: [
      { label: "Khách hàng", to: "/admin/customer", matches: ["/admin/customer"] },
      { label: "Danh mục hỗ trợ", to: "/admin/support-categories", matches: ["/admin/support-categories"] },
      { label: "Thông báo", to: "/admin/announcements", matches: ["/admin/announcements"] },
    ],
  },
  {
    kind: "group",
    label: "Vé và phương tiện",
    icon: "catalog",
    defaultExpanded: true,
    items: [
      { label: "Vé", to: "/admin/ticket", matches: ["/admin/ticket"] },
      { label: "Duyệt & Gán thẻ", to: "/admin/subscription-approvals", matches: ["/admin/subscription-approvals"] },
      { label: "Phương tiện", to: "/admin/vehicle", matches: ["/admin/vehicle"] },
    ],
  },
  {
    kind: "group",
    label: "Bảng giá",
    icon: "pricing",
    items: [
      { label: "Kế hoạch giá", to: "/admin/price-plans", matches: ["/admin/price-plans"] },
      { label: "Quy tắc giá", to: "/admin/price-rules", matches: ["/admin/price-rules"] },
      { label: "Hóa đơn", to: "/admin/invoices", matches: ["/admin/invoices"] },
    ],
  },
  {
    kind: "group",
    label: "Nhân sự",
    icon: "members",
    items: [
      { label: "Nhân viên", to: "/admin/employee", matches: ["/admin/employee"] },
      { label: "Ca trực", to: "/admin/shifts", matches: ["/admin/shifts"] },
    ],
  },
  { kind: "divider" },
  {
    kind: "group",
    label: "Cài đặt hệ thống",
    icon: "settings",
    items: [
      { label: "Tài khoản", to: "/admin/account", matches: ["/admin/account"] },
      { label: "Vai trò & Quyền", to: "/admin/role", matches: ["/admin/role"] },
    ],
  },
];

export const clientNavigation = [
  { label: "Trang chủ", href: "/pricing" },
  { label: "Vào ra", href: "/customerTicket/customer-infor" },
  { label: "Liên hệ", href: "/contact" },
];
