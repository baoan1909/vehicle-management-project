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
      { label: "Yêu cầu hỗ trợ", to: "/admin/support-tickets", matches: ["/admin/support-tickets"] },
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
      { label: "Voucher", to: "/admin/vouchers", matches: ["/admin/vouchers"] },
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
      { label: "Đăng ký đối tác", to: "/admin/partner-registrations", matches: ["/admin/partner-registrations"] },
      { label: "Vai trò & Quyền", to: "/admin/role", matches: ["/admin/role"] },
      { label: "Model AI", to: "/admin/ai-models", matches: ["/admin/ai-models"] },
      { label: "Knowledge AI", to: "/admin/ai-knowledge", matches: ["/admin/ai-knowledge", "/admin/ai-knowledge-index"] },
    ],
  },
];

// Platform navigation uses the existing routes, but groups them by governance scope
// instead of the daily workflow of a single parking lot. Permission filtering remains
// the responsibility of getVisibleAdminNavigation.
export const platformAdminNavigation: AdminSidebarEntry[] = [
  { kind: "section", label: "Giám sát dữ liệu" },
  { kind: "link", label: "Tổng quan", to: "/admin/dashboard", matches: ["/admin/dashboard"], icon: "dashboard", monitoring: true },
  { kind: "link", label: "Đối tác và bãi xe", to: "/admin/parking-lots", matches: ["/admin/parking-lots"], icon: "parking", monitoring: true },
  { kind: "link", label: "Lượt xe, mức lấp đầy", to: "/admin/swipe/sessions", matches: ["/admin/swipe/sessions"], icon: "swipe", monitoring: true },
  { kind: "link", label: "Vé đăng ký", to: "/admin/subscription-approvals", matches: ["/admin/subscription-approvals"], icon: "catalog", monitoring: true },
  { kind: "link", label: "Thẻ", to: "/admin/card", matches: ["/admin/card"], icon: "card", monitoring: true },
  { kind: "link", label: "Doanh thu, hóa đơn", to: "/admin/invoices", matches: ["/admin/invoices"], icon: "pricing", monitoring: true },
  { kind: "link", label: "Nhân sự", to: "/admin/employee", matches: ["/admin/employee"], icon: "members", monitoring: true },
  { kind: "link", label: "Thiết bị", to: "/admin/devices", matches: ["/admin/devices"], icon: "settings", monitoring: true },
  { kind: "planned", label: "Giá gửi xe của bãi (chỉ xem)" },
  { kind: "divider" },
  { kind: "section", label: "Quản trị nền tảng" },
  { kind: "link", label: "Đăng ký đối tác", to: "/admin/partner-registrations", matches: ["/admin/partner-registrations"], icon: "parking" },
  { kind: "link", label: "Khách hàng toàn sàn", to: "/admin/customer", matches: ["/admin/customer"], icon: "members" },
  { kind: "link", label: "Voucher toàn sàn", to: "/admin/vouchers", matches: ["/admin/vouchers"], icon: "catalog" },
  { kind: "planned", label: "Phí dịch vụ sàn" },
  { kind: "planned", label: "Ví điện tử" },
  { kind: "planned", label: "Giao dịch & đối soát" },
  { kind: "link", label: "Tài khoản", to: "/admin/account", matches: ["/admin/account"], icon: "role" },
  { kind: "link", label: "Vai trò & phân quyền", to: "/admin/role", matches: ["/admin/role"], icon: "settings" },
  { kind: "planned", label: "Nhật ký hệ thống" },
  { kind: "link", label: "Hỗ trợ", to: "/admin/support-tickets", matches: ["/admin/support-tickets"], icon: "support" },
  { kind: "link", label: "Model AI", to: "/admin/ai-models", matches: ["/admin/ai-models"], icon: "settings" },
  { kind: "link", label: "Knowledge AI", to: "/admin/ai-knowledge", matches: ["/admin/ai-knowledge", "/admin/ai-knowledge-index"], icon: "catalog" },
];

export const clientNavigation = [
  { label: "Trang chủ", href: "/pricing" },
  { label: "Vào ra", href: "/customerTicket/customer-infor" },
  { label: "Liên hệ", href: "/contact" },
];
